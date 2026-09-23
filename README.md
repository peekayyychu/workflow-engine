```mermaid
flowchart TD

subgraph group_api["API boundary"]
  node_application["Spring application"]
  node_controller["Workflow controller"]
end

subgraph group_runtime["Workflow runtime"]
  node_orchestrator["Workflow orchestrator"]
  node_event_service["Event service"]
  node_decision_engine["Decision engine"]
  node_decision["Workflow decision"]
  node_resolver["Variable resolver"]
end

subgraph group_activities["Activity execution"]
  node_scheduled_event["Scheduled activity event"]
  node_worker["Activity worker"]
  node_handler["Activity handlers"]
  node_http_handler["HTTP activity"]
end

subgraph group_domain["Workflow domain"]
  node_configuration["Workflow configuration"]
  node_definition["Workflow definition"]
  node_step["Workflow step<br/>[WorkflowStep.java]"]
  node_event["Workflow event<br/>[WorkFlowEvent.java]"]
  node_state["Workflow state<br/>[WorkFlowState.java]"]
  node_activity_status["Activity status"]
  node_step_status["Step status<br/>[StepStatus.java]"]
end

subgraph group_persistence["Event persistence"]
  node_repository["Event repository"]
  node_store[("Event database")]
end

node_client(("Workflow client"))
node_external_service["External service"]

node_client -->|"calls API"| node_controller
node_controller -->|"records or reads"| node_event_service
node_orchestrator -->|"loads and records"| node_event_service
node_orchestrator -->|"evaluates workflow"| node_decision_engine
node_decision_engine -->|"produces decision"| node_decision
node_decision_engine -->|"reads history"| node_state
node_decision_engine -->|"evaluates steps"| node_definition
node_decision_engine -->|"aggregates status"| node_activity_status
node_orchestrator -->|"resolves inputs"| node_resolver
node_resolver -->|"reads activity outputs"| node_state
node_orchestrator -->|"publishes activity"| node_scheduled_event
node_scheduled_event -->|"dispatches event"| node_worker
node_worker -->|"dispatches activity"| node_handler
node_worker -->|"records outcome"| node_event_service
node_worker -->|"reprocesses workflow"| node_orchestrator
node_http_handler -.->|"calls HTTP service"| node_external_service
node_configuration -->|"registers definition"| node_definition
node_definition -->|"defines steps"| node_step
node_event_service -->|"reads and writes"| node_repository
node_repository -->|"persists events"| node_store
node_event_service -->|"creates events"| node_event
node_event_service -->|"builds state"| node_state

click node_application "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/WorkflowEngineApplication.java"
click node_controller "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/controller/WorkFlowController.java"
click node_orchestrator "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/service/WorkFlowOrchestrator.java"
click node_event_service "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/service/WorkFlowEventService.java"
click node_decision_engine "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/service/WorkFlowDecisionEngine.java"
click node_decision "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/service/WorkFlowDecision.java"
click node_resolver "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/resolver/VariableResolver.java"
click node_scheduled_event "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/ActivityScheduledEvent.java"
click node_worker "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/activity/ActivityWorkerListener.java"
click node_handler "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/activity/ActivityHandler.java"
click node_http_handler "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/activity/HttpActivityHandler.java"
click node_configuration "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/registry/WorkflowConfiguration.java"
click node_definition "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/WorkflowDefinition.java"
click node_step "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/WorkflowStep.java"
click node_event "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/WorkFlowEvent.java"
click node_state "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/WorkFlowState.java"
click node_activity_status "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/enums/ActivityStatus.java"
click node_step_status "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/model/enums/StepStatus.java"
click node_repository "https://github.com/peekayyychu/workflow-engine/blob/main/src/main/java/com/engine/repository/WorkFlowEventRepository.java"

classDef toneNeutral fill:#f8fafc,stroke:#334155,stroke-width:1.5px,color:#0f172a
classDef toneBlue fill:#dbeafe,stroke:#2563eb,stroke-width:1.5px,color:#172554
classDef toneAmber fill:#fef3c7,stroke:#d97706,stroke-width:1.5px,color:#78350f
classDef toneMint fill:#dcfce7,stroke:#16a34a,stroke-width:1.5px,color:#14532d
classDef toneRose fill:#ffe4e6,stroke:#e11d48,stroke-width:1.5px,color:#881337
classDef toneIndigo fill:#e0e7ff,stroke:#4f46e5,stroke-width:1.5px,color:#312e81
classDef toneTeal fill:#ccfbf1,stroke:#0f766e,stroke-width:1.5px,color:#134e4a
class node_application,node_controller,node_client toneBlue
class node_orchestrator,node_event_service,node_decision_engine,node_decision,node_resolver toneAmber
class node_scheduled_event,node_worker,node_handler,node_http_handler toneMint
class node_configuration,node_definition,node_step,node_event,node_state,node_activity_status,node_step_status toneRose
class node_repository,node_store,node_external_service toneIndigo
```