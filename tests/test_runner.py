import json
import logging
import sys
import time
import uuid
import requests

# Set non-interactive matplotlib backend for headless execution support
try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import matplotlib.patches as mpatches
    MATPLOTLIB_AVAILABLE = True
except ImportError:
    MATPLOTLIB_AVAILABLE = False

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
BASE_URL = "http://localhost:8081/api/v1/workflows"
WORKFLOW_ID = f"wf-e2e-{uuid.uuid4().hex[:8]}"

# Interaction tracker for graph generation
interaction_history = []

# Logger 1: Stores all API request/response payloads
response_logger = logging.getLogger("API_Responses")
response_logger.setLevel(logging.INFO)
resp_handler = logging.FileHandler("api_responses.log", mode="w")
resp_handler.setFormatter(logging.Formatter("%(asctime)s - %(message)s"))
response_logger.addHandler(resp_handler)

# Logger 2: Stores non-2xx errors or exceptions
error_logger = logging.getLogger("Error_Logs")
error_logger.setLevel(logging.ERROR)
err_handler = logging.FileHandler("errors.log", mode="w")
err_handler.setFormatter(
    logging.Formatter("%(asctime)s - [%(levelname)s] - %(message)s")
)
error_logger.addHandler(err_handler)


def log_api_call(
    step_num: int,
    step_name: str,
    method: str,
    url: str,
    request_body,
    response: requests.Response,
):
    try:
        resp_payload = response.json() if response.text else None
    except Exception:
        resp_payload = response.text

    log_entry = {
        "step": step_num,
        "step_name": step_name,
        "method": method,
        "url": url,
        "status_code": response.status_code,
        "request_body": request_body,
        "response_body": resp_payload,
    }
    response_logger.info(json.dumps(log_entry, indent=2))

    # Record interaction for graph rendering
    interaction_history.append(
        {
            "step": step_num,
            "step_name": step_name,
            "method": method,
            "endpoint": url.replace(BASE_URL, ""),
            "status_code": response.status_code,
            "success": response.ok,
        }
    )

    if not response.ok:
        err_msg = f"API Call Failed: {method} {url} | Status: {response.status_code} | Body: {response.text}"
        error_logger.error(err_msg)
        print(f"   ❌ [ERROR] Status {response.status_code}: {response.text}")


start_request_payload = {
    "definition": {
        "steps": [
            {
                "activityName": "fetchUserData",
                "defaultInput": {"userId": "usr_999"},
                "retryCount": 1,
            },
            {
                "activityName": "sendEmailNotification",
                "defaultInput": {"email": "test@example.com"},
                "retryCount": 1,
            },
        ]
    },
    "payload": json.dumps({"env": "staging", "initiatedBy": "e2e_runner"}),
}


# ---------------------------------------------------------------------------
# Visualization Generator Functions
# ---------------------------------------------------------------------------
def print_ascii_graph():
    """Prints a terminal-friendly visual ASCII flow graph."""
    print("\n" + "=" * 66)
    print(" 📊 WORKFLOW EXECUTION & INTERACTION FLOW (ASCII)")
    print("=" * 66)
    ascii_art = """
 +--------------------+            +---------------------------------+
 |  E2E Test Runner   | ---------> |  Spring Boot Orchestrator API   |
 +--------------------+            +---------------------------------+
           |                                  |
   (1) POST /start                            | [Start Workflow]
           |--------------------------------->|
           |                                  |---> [State: RUNNING]
   (2) POST /fetchUserData/complete           |
           |--------------------------------->|---> [fetchUserData: COMPLETED ✅]
           |                                  |
   (3) POST /sendEmailNotification/fail       |
           |--------------------------------->|---> [sendEmailNotification: FAILED ❌]
           |                                  |
   (4) POST /resume                           |
           |--------------------------------->|---> [Re-evaluate Orchestrator 🔄]
           |                                  |
   (5/6) GET /state & /history                |
           |<---------------------------------|---> [Fetch State & Audit Log]
    """
    print(ascii_art)


def generate_png_graph(output_filename="workflow_execution_graph.png"):
    """Generates a PNG image containing interaction sequence and state flow."""
    if not MATPLOTLIB_AVAILABLE:
        print("⚠️ Matplotlib not installed. Skipping PNG graph generation.")
        return

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 10))
    fig.suptitle(
        f"Workflow Execution Graph (ID: {WORKFLOW_ID})",
        fontsize=14,
        fontweight="bold",
    )

    # 1. Component Interaction Sequence Plot
    components = [
        "E2E Runner",
        "Orchestrator API",
        "fetchUserData",
        "sendEmailNotification",
    ]
    y_map = {comp: idx for idx, comp in enumerate(components)}

    ax1.set_title("1. Component API Interaction Timeline", fontsize=11)
    ax1.set_yticks(list(y_map.values()))
    ax1.set_yticklabels(components, fontweight="bold")
    ax1.set_xlabel("Execution Steps")
    ax1.grid(True, linestyle="--", alpha=0.5)

    for item in interaction_history:
        x = item["step"]
        color = "#2ca02c" if item["success"] else "#d62728"

        # Determine target component
        target = "Orchestrator API"
        if "fetchUserData" in item["endpoint"]:
            target = "fetchUserData"
        elif "sendEmailNotification" in item["endpoint"]:
            target = "sendEmailNotification"

        ax1.annotate(
            f"{item['method']} {item['endpoint']}\n[{item['status_code']}]",
            xy=(x, y_map[target]),
            xytext=(x, y_map["E2E Runner"]),
            arrowprops=dict(
                arrowstyle="->", color=color, lw=2, mutation_scale=15
            ),
            ha="center",
            va="bottom",
            fontsize=8,
            bbox=dict(
                boxstyle="round,pad=0.3",
                fc="white",
                ec=color,
                lw=1,
                alpha=0.9,
            ),
        )

    ax1.set_xlim(0, len(interaction_history) + 1)
    ax1.set_ylim(-0.5, len(components) - 0.5)

    # 2. Workflow State Transition DAG
    ax2.set_title("2. Workflow Step Execution State Flow", fontsize=11)
    ax2.axis("off")

    nodes = [
        {"name": "Start\nWorkflow", "status": "COMPLETED", "x": 1, "y": 1},
        {"name": "fetchUserData\n(Step 1)", "status": "VERIFIED", "x": 3, "y": 1},
        {
            "name": "sendEmailNotification\n(Step 2)",
            "status": "FAILED",
            "x": 5,
            "y": 1,
        },
        {"name": "Resume\nOrchestrator", "status": "RESUMED", "x": 7, "y": 1},
        {"name": "Audit & State\nFetch", "status": "COMPLETED", "x": 9, "y": 1},
    ]

    for node in nodes:
        bg_color = (
            "#c8e6c9"
            if node["status"] in ["COMPLETED", "VERIFIED"]
            else "#ffcdd2"
            if node["status"] == "FAILED"
            else "#bbdefb"
        )
        border_color = (
            "#2e7d32"
            if node["status"] in ["COMPLETED", "VERIFIED"]
            else "#c62828"
            if node["status"] == "FAILED"
            else "#1565c0"
        )

        ax2.text(
            node["x"],
            node["y"],
            f"{node['name']}\n[{node['status']}]",
            ha="center",
            va="center",
            bbox=dict(
                boxstyle="round,pad=0.8",
                facecolor=bg_color,
                edgecolor=border_color,
                linewidth=2,
            ),
            fontsize=9,
            fontweight="bold",
        )

    # Draw transition arrows
    for i in range(len(nodes) - 1):
        ax2.annotate(
            "",
            xy=(nodes[i + 1]["x"] - 0.7, 1),
            xytext=(nodes[i]["x"] + 0.7, 1),
            arrowprops=dict(
                arrowstyle="-|>", color="#424242", lw=2, mutation_scale=15
            ),
        )

    ax2.set_xlim(0, 10)
    ax2.set_ylim(0, 2)

    plt.tight_layout()
    plt.savefig(output_filename, dpi=300)
    plt.close()
    print(f"📸 Image Graph saved to: {output_filename}")


def generate_mermaid_html(output_filename="workflow_diagram.html"):
    """Generates an HTML file rendering an interactive Mermaid.js diagram."""
    mermaid_seq_lines = []
    for item in interaction_history:
        status_str = f"{item['status_code']} ({'OK' if item['success'] else 'FAIL'})"
        mermaid_seq_lines.append(
            f"    Runner->>Orchestrator: {item['method']} {item['endpoint']}"
        )
        mermaid_seq_lines.append(
            f"    Orchestrator-->>Runner: Response Code {status_str}"
        )

    seq_content = "\n".join(mermaid_seq_lines)

    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <title>Workflow Execution Graph</title>
    <script type="module">
        import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
        mermaid.initialize({{ startOnLoad: true, theme: 'forest' }});
    </script>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 30px; background-color: #f9f9f9; }}
        .card {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 25px; }}
        h2 {{ color: #333; margin-top: 0; }}
    </style>
</head>
<body>
    <h1>🚀 Workflow Execution & Interaction Report</h1>
    <p><b>Workflow Instance ID:</b> {WORKFLOW_ID}</p>

    <div class="card">
        <h2>1. Interaction Sequence Diagram</h2>
        <pre class="mermaid">
sequenceDiagram
    autonumber
    actor Runner as E2E Test Runner
    participant Orchestrator as Spring Boot API
{seq_content}
        </pre>
    </div>

    <div class="card">
        <h2>2. Workflow Activity State Flow</h2>
        <pre class="mermaid">
graph LR
    A[Start Workflow] --> B[Activity: fetchUserData]
    B -->|Verified| C[Activity: sendEmailNotification]
    C -->|SMTP Timeout| D[Resume Engine]
    D --> E[Fetch Audit State & History]

    style A fill:#bbdefb,stroke:#1565c0,stroke-width:2px
    style B fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px
    style C fill:#ffcdd2,stroke:#c62828,stroke-width:2px
    style D fill:#ffe0b2,stroke:#ef6c00,stroke-width:2px
    style E fill:#e1bee7,stroke:#6a1b9a,stroke-width:2px
        </pre>
    </div>
</body>
</html>
"""
    with open(output_filename, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"🌐 Interactive HTML Graph saved to: {output_filename}")


# ---------------------------------------------------------------------------
# Execution
# ---------------------------------------------------------------------------
def run_e2e_test():
    headers = {"Content-Type": "application/json"}

    print("\n==================================================================")
    print(f"🚀 Running E2E Workflow Test | Instance ID: {WORKFLOW_ID}")
    print("==================================================================\n")

    # 1. Start Workflow
    url = f"{BASE_URL}/{WORKFLOW_ID}/start"
    print("1. [POST] /start - Starting Workflow Instance...")
    try:
        resp = requests.post(url, json=start_request_payload, headers=headers)
        log_api_call(1, "Start Workflow", "POST", url, start_request_payload, resp)
        print(f"   Status: {resp.status_code}")
    except Exception as e:
        print(f"❌ Could not connect to Spring Boot server: {str(e)}")
        sys.exit(1)

    time.sleep(0.5)

    # 2. Complete Activity
    url = f"{BASE_URL}/{WORKFLOW_ID}/activity/fetchUserData/complete"
    print("\n2. [POST] /activity/fetchUserData/complete - Completing Step 1...")
    payload_str = json.dumps({"userId": "usr_999", "status": "VERIFIED"})
    resp = requests.post(url, data=payload_str, headers=headers)
    log_api_call(2, "Complete Step 1", "POST", url, payload_str, resp)
    print(f"   Status: {resp.status_code}")

    # 3. Fail Activity
    url = f"{BASE_URL}/{WORKFLOW_ID}/activity/sendEmailNotification/fail"
    print("\n3. [POST] /activity/sendEmailNotification/fail - Simulating Failure...")
    fail_payload = json.dumps({"error": "SMTP server connection timeout"})
    resp = requests.post(url, data=fail_payload, headers=headers)
    log_api_call(3, "Fail Step 2", "POST", url, fail_payload, resp)
    print(f"   Status: {resp.status_code}")

    # 4. Resume Workflow
    url = f"{BASE_URL}/{WORKFLOW_ID}/resume"
    print("\n4. [POST] /resume - Re-evaluating Orchestrator...")
    resp = requests.post(url, headers=headers)
    log_api_call(4, "Resume Workflow", "POST", url, None, resp)
    print(f"   Status: {resp.status_code}")

    # 5. Fetch State
    url = f"{BASE_URL}/{WORKFLOW_ID}/state"
    print("\n5. [GET] /state - Fetching State...")
    resp = requests.get(url)
    log_api_call(5, "Fetch State", "GET", url, None, resp)
    print(f"   Status: {resp.status_code}")

    # 6. Fetch History
    url = f"{BASE_URL}/{WORKFLOW_ID}/history"
    print("\n6. [GET] /history - Fetching Audit History...")
    resp = requests.get(url)
    log_api_call(6, "Fetch History", "GET", url, None, resp)
    print(f"   Status: {resp.status_code}")

    # Generate Graphs
    print_ascii_graph()
    generate_png_graph()
    generate_mermaid_html()

    print("\n==================================================================")
    print("✅ Execution Complete.")
    print("   - API Logs: api_responses.log & errors.log")
    print("   - PNG Chart: workflow_execution_graph.png")
    print("   - Interactive Diagram: workflow_diagram.html")
    print("==================================================================\n")


if __name__ == "__main__":
    run_e2e_test()