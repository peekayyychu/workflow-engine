import json
import logging
import sys
import time
import uuid
import requests

# ---------------------------------------------------------------------------
# Configuration & Logging Setup
# ---------------------------------------------------------------------------
BASE_URL = "http://localhost:8080/api/v1/workflows"
WORKFLOW_ID = f"wf-e2e-{uuid.uuid4().hex[:8]}"

# Logger 1: Stores all API request/response payloads in api_responses.log
response_logger = logging.getLogger("API_Responses")
response_logger.setLevel(logging.INFO)
resp_handler = logging.FileHandler("api_responses.log", mode="w")
resp_handler.setFormatter(logging.Formatter("%(asctime)s - %(message)s"))
response_logger.addHandler(resp_handler)

# Logger 2: Stores all non-2xx errors or exceptions in errors.log
error_logger = logging.getLogger("Error_Logs")
error_logger.setLevel(logging.ERROR)
err_handler = logging.FileHandler("errors.log", mode="w")
err_handler.setFormatter(
    logging.Formatter("%(asctime)s - [%(levelname)s] - %(message)s")
)
error_logger.addHandler(err_handler)


def log_api_call(method: str, url: str, request_body, response: requests.Response):
    """Logs full request/response context and traps non-2xx errors."""
    try:
        resp_payload = response.json() if response.text else None
    except Exception:
        resp_payload = response.text

    log_entry = {
        "method": method,
        "url": url,
        "status_code": response.status_code,
        "request_body": request_body,
        "response_body": resp_payload,
    }
    response_logger.info(json.dumps(log_entry, indent=2))

    if not response.ok:
        err_msg = f"API Call Failed: {method} {url} | Status: {response.status_code} | Body: {response.text}"
        error_logger.error(err_msg)
        print(f"   ❌ [ERROR] Status {response.status_code}: {response.text}")


# ---------------------------------------------------------------------------
# Test Workflow Definition & Start Request Payload
# ---------------------------------------------------------------------------
start_request_payload = {
    "definition": {
        "steps": [
            {
                "activityName": "fetchUserData",
                "defaultInput": {"userId": "usr_999"},
                "retryCount": 1,
            },
            {
                "activityName": "TIMER_WAIT_3S",
                "defaultInput": {"durationSeconds": 3},
                "retryCount": 0,
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
# E2E Execution Test Sequence
# ---------------------------------------------------------------------------
def run_e2e_test():
    print(
        f"\n=================================================================="
    )
    print(f"🚀 Running E2E Workflow Test | Instance ID: {WORKFLOW_ID}")
    print(
        f"==================================================================\n"
    )

    # 1. Start Workflow (POST /{workflowId}/start)
    url = f"{BASE_URL}/{WORKFLOW_ID}/start"
    print("1. [POST] /start - Starting Workflow Instance...")
    try:
        resp = requests.post(url, json=start_request_payload)
        log_api_call("POST", url, start_request_payload, resp)
        print(f"   Status: {resp.status_code}")
    except Exception as e:
        err_msg = f"Failed to reach Spring Boot server at {url}: {str(e)}"
        error_logger.error(err_msg)
        print(f"❌ Server connection failed. Is Spring Boot running on port 8080?")
        sys.exit(1)

    time.sleep(0.5)

    # 2. Complete First Activity (POST /{workflowId}/activity/fetchUserData/complete)
    url = f"{BASE_URL}/{WORKFLOW_ID}/activity/fetchUserData/complete"
    print("\n2. [POST] /activity/fetchUserData/complete - Completing Step 1...")
    payload_str = json.dumps({"userId": "usr_999", "status": "VERIFIED"})
    resp = requests.post(
        url, data=payload_str, headers={"Content-Type": "text/plain"}
    )
    log_api_call("POST", url, payload_str, resp)
    print(f"   Status: {resp.status_code}")

    # 3. Simulate Activity Failure (POST /{workflowId}/activity/sendEmailNotification/fail)
    url = f"{BASE_URL}/{WORKFLOW_ID}/activity/sendEmailNotification/fail"
    print(
        "\n3. [POST] /activity/sendEmailNotification/fail - Simulating Failure..."
    )
    fail_payload = json.dumps({"error": "SMTP server connection timeout"})
    resp = requests.post(
        url, data=fail_payload, headers={"Content-Type": "text/plain"}
    )
    log_api_call("POST", url, fail_payload, resp)
    print(f"   Status: {resp.status_code}")

    # 4. Resume Workflow (POST /{workflowId}/resume)
    url = f"{BASE_URL}/{WORKFLOW_ID}/resume"
    print("\n4. [POST] /resume - Re-evaluating Orchestrator from Redis...")
    resp = requests.post(url)
    log_api_call("POST", url, None, resp)
    print(f"   Status: {resp.status_code}")

    # 5. Fetch Workflow State (GET /{workflowId}/state)
    url = f"{BASE_URL}/{WORKFLOW_ID}/state"
    print("\n5. [GET] /state - Fetching Current State...")
    resp = requests.get(url)
    log_api_call("GET", url, None, resp)
    print(f"   Status: {resp.status_code}")

    # 6. Fetch Event Audit Log (GET /{workflowId}/history)
    url = f"{BASE_URL}/{WORKFLOW_ID}/history"
    print("\n6. [GET] /history - Fetching Audit History...")
    resp = requests.get(url)
    log_api_call("GET", url, None, resp)
    print(f"   Status: {resp.status_code}")

    print(
        "\n=================================================================="
    )
    print("✅ E2E Test Execution Completed Successfully!")
    print("📄 Full API Payloads Logged: api_responses.log")
    print("📄 Error Logs Written     : errors.log")
    print(
        "==================================================================\n"
    )


if __name__ == "__main__":
    run_e2e_test()