# GitHub Repository Analyzer

A Java application that analyzes GH repos to:
- display **project directory tree**
- calculate **file level metrics**: 
   - size
   - complexity
- calculate **class level metrics**:
   - abstractness 
   - instability 
   - distance
- generate **UML class diagrams**

## How to Run

1. Set your GitHub token in `.env`:
   ```
   GITHUB_TOKEN=your_token_here
   ```

2. Compile and run:
   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass="Asgn3.Main"
   ```

3. On GUI: enter a GH repository URL (must point to a `/src` directory)

4. View results in the Grid, Metrics, and Diagram tabs

5. Additional features included in menu bar: 
   - file 
      - open from URL: analyze a new repo (has same functionality as 'ok' button next to where you insert a GH repo)
      - exit: close application 
   - action
      - reload: re-analyze current repo
      - clear: clear all results and reset application 
   - help
      - about: application info

---
# CLASS DIAGRAM 

**[UML Diagram](https://www.mermaidchart.com/d/6e540e71-043a-4e22-beee-6400c3d738fe)**
OR 
**Can be found in:** src/main/resources/Class-Diagram.png

**Mermaid Code:**

```mermaid
classDiagram
    %% UI Layer - Main Entry Point
    class Main {
        <<entry point>>
    }
    
    %% UI Layer - Main Frame
    class MainFrame {
        <<JFrame>>
    }
    
    %% UI Components
    class MenuBar {
        <<JMenuBar>>
    }
    
    class TopBar {
        <<JPanel>>
    }
    
    class BottomPanel {
        <<JPanel>>
    }
    
    class SidePanel {
        <<JPanel>>
    }
    
    class GridPanel {
        <<JPanel>>
    }
    
    class MetricsPanel {
        <<JPanel>>
    }
    
    class DiagramPanel {
        <<JPanel>>
    }
    
    %% GitHub Operations
    class GHOperations {
    }
    
    class GHRepoAnalyzer {
    }
    
    class GHInfo {
        <<record>>
    }
    
    %% Analysis Results
    class GHRepoAnalyzed {
    }
    
    class FileLevelMetrics {
    }
    
    class ClassLevelMetrics {
    }
    
    class JavaClass {
    }
    
    %% Utilities
    class EnvLoader {
        <<utility>>
    }
    
    %% Relationships - Main Entry
    Main ..> MainFrame : creates
    
    %% MainFrame Composition (has-a)
    MainFrame *-- MenuBar : has
    MainFrame *-- TopBar : has
    MainFrame *-- BottomPanel : has
    MainFrame *-- SidePanel : has
    MainFrame *-- GridPanel : has
    MainFrame *-- MetricsPanel : has
    MainFrame *-- DiagramPanel : has
    MainFrame *-- GHOperations : has
    
    %% MainFrame Dependencies
    MainFrame ..> GHRepoAnalyzer : uses
    MainFrame ..> GHRepoAnalyzed : uses
    MainFrame ..> EnvLoader : uses
    
    %% Panel Dependencies
    GridPanel o-- BottomPanel : uses
    GridPanel ..> FileLevelMetrics : displays
    MetricsPanel o-- BottomPanel : uses
    MetricsPanel ..> ClassLevelMetrics : displays
    
    %% MenuBar Dependencies
    MenuBar ..> MainFrame : references
    
    %% Analyzer Layer
    GHRepoAnalyzer *-- GHOperations : has
    GHRepoAnalyzer ..> GHInfo : uses
    GHRepoAnalyzer ..> GHRepoAnalyzed : creates
    GHRepoAnalyzer ..> FileLevelMetrics : creates
    GHRepoAnalyzer ..> ClassLevelMetrics : creates
    GHRepoAnalyzer ..> JavaClass : creates
    
    %% GitHub Operations
    GHOperations ..> GHInfo : creates
    
    %% Analysis Result Composition
    GHRepoAnalyzed *-- FileLevelMetrics : contains
    GHRepoAnalyzed *-- ClassLevelMetrics : contains
```

---

# SAMPLE TESTER GH URL 

**[GH](https://github.com/meganfung38/sample/tree/main/src)**
