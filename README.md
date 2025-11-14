# README 
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
