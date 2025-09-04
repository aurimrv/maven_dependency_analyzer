package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class App {

    private static boolean showParameters = false;
    private static Set<String> internalClasses = new HashSet<>();

    public static void main(String[] args) {
        String projectPath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--deps-with-parameters")) {
                showParameters = true;
            } else if (args[i].equals("--project")) {
                if (i + 1 < args.length) {
                    projectPath = args[++i];
                } else {
                    System.err.println("Erro: O parâmetro --project requer um caminho.");
                    return;
                }
            }
        }

        if (projectPath == null) {
            System.err.println("Erro: O parâmetro --project é obrigatório. Ex: java -jar seu-analisador.jar --project /caminho/para/seu/projeto/maven");
            return;
        }

        File projectRoot = new File(projectPath);
        File sourceDir = new File(projectRoot, "src/main/java");

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.err.println("Diretório de código fonte Java não encontrado: " + sourceDir.getAbsolutePath());
            return;
        }

        List<File> javaFiles = listJavaFiles(sourceDir);
        if (javaFiles.isEmpty()) {
            System.out.println("Nenhum arquivo Java encontrado em: " + sourceDir.getAbsolutePath());
            return;
        }

        // First pass: collect all internal class names
        for (File file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                    internalClasses.add(c.getNameAsString());
                });
            } catch (FileNotFoundException e) {
                System.err.println("Arquivo não encontrado: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Erro ao analisar o arquivo " + file.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Analisando " + javaFiles.size() + " arquivos Java...");

        // Second pass: analyze dependencies
        for (File file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                System.out.println("\n--- Arquivo: " + file.getName() + " ---");
                cu.accept(new DependencyVisitor(), null);
            } catch (FileNotFoundException e) {
                System.err.println("Arquivo não encontrado: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Erro ao analisar o arquivo " + file.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static List<File> listJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(listJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }

    private static class DependencyVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            super.visit(n, arg);
            String className = n.findAncestor(ClassOrInterfaceDeclaration.class)
                                  .map(ClassOrInterfaceDeclaration::getNameAsString)
                                  .orElse("UnknownClass");
            String methodSignature = className + "." + n.getNameAsString() + getParametersString(n.getParameters());
            System.out.println("  Método: " + methodSignature);
            n.findAll(MethodCallExpr.class).forEach(mc -> {
                String calledMethodName = mc.getNameAsString();
                String scope = mc.getScope().map(s -> s.toString() + ".").orElse("");
                String dependencyType = "";

                // Heuristic to determine if it's an internal or external call
                // This is a simplification and might need more robust type resolution for complex cases
                if (mc.getScope().isPresent()) {
                    String scopeName = mc.getScope().get().toString();
                    if (internalClasses.contains(scopeName)) {
                        dependencyType = "(Interna)";
                    } else {
                        dependencyType = "(Externa)";
                    }
                } else {
                    // If no explicit scope, assume it's an internal call (e.g., 'this' or same class)
                    dependencyType = "(Interna)";
                }

                System.out.println("    Chamada de método: " + scope + calledMethodName + " " + dependencyType);
            });
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            super.visit(n, arg);
            String className = n.findAncestor(ClassOrInterfaceDeclaration.class)
                                  .map(ClassOrInterfaceDeclaration::getNameAsString)
                                  .orElse("UnknownClass");
            String constructorSignature = className + "." + n.getNameAsString() + getParametersString(n.getParameters());
            System.out.println("  Construtor: " + constructorSignature);
            n.findAll(MethodCallExpr.class).forEach(mc -> {
                String calledMethodName = mc.getNameAsString();
                String scope = mc.getScope().map(s -> s.toString() + ".").orElse("");
                String dependencyType = "";

                if (mc.getScope().isPresent()) {
                    String scopeName = mc.getScope().get().toString();
                    if (internalClasses.contains(scopeName)) {
                        dependencyType = "(Interna)";
                    } else {
                        dependencyType = "(Externa)";
                    }
                } else {
                    dependencyType = "(Interna)";
                }
                System.out.println("    Chamada de método: " + scope + calledMethodName + " " + dependencyType);
            });
        }

        private String getParametersString(List<Parameter> parameters) {
            if (!showParameters) {
                return "(...)";
            }
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < parameters.size(); i++) {
                sb.append(parameters.get(i).getType().asString());
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }
}


