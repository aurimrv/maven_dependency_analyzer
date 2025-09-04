# Analisador de Dependências Java

Este projeto é um analisador de dependências para projetos Java Maven. Ele identifica chamadas de métodos dentro do seu código e as classifica como dependências internas (dentro do seu projeto) ou externas (de bibliotecas de terceiros).

## Como usar

### Pré-requisitos

Certifique-se de ter o Maven e o JDK (Java Development Kit) instalados em sua máquina.

### Compilação

Para compilar o projeto, navegue até o diretório raiz do projeto `dependency-analyzer` e execute o seguinte comando:

```bash
mvn clean install
```

Este comando irá compilar o código e empacotá-lo em um arquivo JAR executável no diretório `target/`.

### Execução

Após a compilação, você pode executar o analisador usando o seguinte comando, especificando a raiz do seu projeto Maven com `--project`:

```bash
java -jar target/dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar --project /caminho/para/seu/projeto/maven
```

Substitua `/caminho/para/seu/projeto/maven` pelo caminho absoluto para a raiz do seu projeto Maven (o diretório que contém o `pom.xml` principal).

Por padrão, o analisador exibirá as dependências sem os detalhes dos parâmetros do método. Para incluir os tipos de parâmetros, use a flag `--deps-with-parameters`:

```bash
java -jar target/dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar --project /caminho/para/seu/projeto/maven --deps-with-parameters
```

## Exemplo de Saída

```
--- Arquivo: VendingMachine.java ---
  Método: VendingMachine.VendingMachine(...)
    Chamada de método: Dispenser.Dispenser (Interna)
    Chamada de método: StockPrice.StockPrice (Interna)
  Método: VendingMachine.selectItem(...)
    Chamada de método: Dispenser.getItem (Interna)
    Chamada de método: StockPrice.getPrice (Interna)
    Chamada de método: VendingMachine.checkCredit (Interna)
    Chamada de método: Dispenser.dispense (Interna)
    Chamada de método: VendingMachine.updateCredit (Interna)
```

Com a flag `--deps-with-parameters`:

```
--- Arquivo: VendingMachine.java ---
  Método: VendingMachine.VendingMachine(int, int)
    Chamada de método: Dispenser.Dispenser(int) (Interna)
    Chamada de método: StockPrice.StockPrice(int) (Interna)
  Método: VendingMachine.selectItem(java.lang.String)
    Chamada de método: Dispenser.getItem(java.lang.String) (Interna)
    Chamada de método: StockPrice.getPrice(java.lang.String) (Interna)
    Chamada de método: VendingMachine.checkCredit(double) (Interna)
    Chamada de método: Dispenser.dispense(java.lang.String) (Interna)
    Chamada de método: VendingMachine.updateCredit(double) (Interna)
```

