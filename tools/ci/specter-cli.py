#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
1C:Specter CLI Runner
Headless test execution runner for 1C:Specter (UIxUnit) with JUnit XML reporting.
Pure Python standard library implementation (no external dependencies).
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser(
        description="1C:Specter Headless CLI Test Runner (CI/CD Integration)",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--out-dir",
        required=True,
        help="Directory for bridge file exchange between runner and 1C client",
    )
    parser.add_argument(
        "--module",
        default="",
        help="Target test suite module name (e.g. 'OZON_UI_Тесты_Контрагенты')",
    )
    parser.add_argument(
        "--test",
        default="",
        help="Specific test method name (optional)",
    )
    parser.add_argument(
        "--client-path",
        default="1cv8c",
        help="Path to 1C thin client executable (1cv8c / 1cv8.exe)",
    )
    parser.add_argument(
        "--ib-conn",
        required=True,
        help="1C connection string: 'File=\"/path/to/ib\"' or 'Srvr=\"host\";Ref=\"db\"' or '/F \"path\"'",
    )
    parser.add_argument(
        "--user",
        "-u",
        default="",
        help="1C username (/N)",
    )
    parser.add_argument(
        "--password",
        "-p",
        default="",
        help="1C password (/P)",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="Maximum timeout in seconds to wait for test run completion",
    )
    parser.add_argument(
        "--report-path",
        default="junit-report.xml",
        help="Path where JUnit XML report will be generated",
    )
    parser.add_argument(
        "--keep-exchange",
        action="store_true",
        help="Do not delete exchange files after run",
    )
    return parser.parse_args()


def atomic_write_json(file_path: Path, data: dict):
    """Writes a JSON file atomically using a temporary file and atomic rename."""
    file_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = file_path.with_suffix(file_path.suffix + ".tmp")
    
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.flush()
        os.fsync(f.fileno())

    # Atomic rename/replace
    tmp_path.replace(file_path)


def read_json_safe(file_path: Path, max_attempts=5, delay=0.05):
    """Safely reads a JSON file with retries to avoid reading half-written files."""
    for attempt in range(max_attempts):
        if not file_path.exists():
            time.sleep(delay)
            continue
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if not content:
                    time.sleep(delay)
                    continue
                return json.loads(content)
        except (json.JSONDecodeError, OSError):
            time.sleep(delay)
    return None


def build_1c_command(client_path, ib_conn, out_dir_str, user="", password=""):
    """Builds the command-line invocation arguments for 1C thin client."""
    cmd = [client_path, "ENTERPRISE"]

    # Connection parameter
    conn_str = ib_conn.strip()
    if conn_str.startswith("/F ") or conn_str.startswith("/S ") or conn_str.startswith("/IBConnectionString "):
        cmd.extend(conn_str.split(" ", 1))
    elif conn_str.startswith("File=") or conn_str.startswith("Srvr="):
        cmd.extend(["/IBConnectionString", conn_str])
    else:
        cmd.extend(["/IBConnectionString", f'File="{conn_str}"'])

    # Credentials
    if user:
        cmd.extend(["/N", user])
    if password:
        cmd.extend(["/P", password])

    # Startup parameter for Specter BSL bridge
    cmd.extend(["/C", f"SPECTER_START_BRIDGE|outDir={out_dir_str}"])

    return cmd


def generate_junit_xml(suite_name: str, run_id: str, status: str, steps: list, total_duration: float, output_path: Path, error_message: str = ""):
    """Generates a standard JUnit XML report compatible with GitLab CI, Jenkins, GitHub Actions."""
    total_tests = len(steps)
    failures = 0
    errors = 0

    if total_tests == 0:
        total_tests = 1
        if status != "passed":
            failures = 1

    for step in steps:
        st = str(step.get("status", "")).lower()
        if st in ("failed", "failure"):
            failures += 1
        elif st in ("error", "aborted", "timeout"):
            errors += 1

    testsuites = ET.Element("testsuites", {
        "name": "Specter 1C UI Tests",
        "tests": str(total_tests),
        "failures": str(failures),
        "errors": str(errors),
        "time": f"{total_duration:.3f}",
    })

    testsuite = ET.SubElement(testsuites, "testsuite", {
        "name": suite_name or "SpecterSuite",
        "tests": str(total_tests),
        "failures": str(failures),
        "errors": str(errors),
        "time": f"{total_duration:.3f}",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()),
        "id": run_id,
    })

    if not steps:
        # Fallback single test case
        tc_name = suite_name or "Run"
        testcase = ET.SubElement(testsuite, "testcase", {
            "name": tc_name,
            "classname": f"1C.{suite_name or 'Specter'}",
            "time": f"{total_duration:.3f}",
        })
        if status != "passed":
            fail_el = ET.SubElement(testcase, "failure", {
                "message": error_message or f"Test execution status: {status}",
                "type": "SpecterExecutionFailure",
            })
            fail_el.text = error_message or f"Status: {status}\nNo steps were executed."
    else:
        for idx, step in enumerate(steps, 1):
            action = step.get("action", f"step_{idx}")
            locator = step.get("locator", "")
            tc_name = locator if locator else (f"{action} [{step.get('id', idx)}]" if step.get("id") else action)
            step_status = str(step.get("status", "")).lower()
            detail = step.get("detail") or step.get("message") or step.get("error") or ""
            step_time = float(step.get("durationMs", 0)) / 1000.0

            testcase = ET.SubElement(testsuite, "testcase", {
                "name": tc_name,
                "classname": f"1C.{suite_name or 'Specter'}",
                "time": f"{step_time:.3f}",
            })

            if step_status in ("failed", "failure"):
                fail_el = ET.SubElement(testcase, "failure", {
                    "message": detail or "Step failed",
                    "type": "AssertionFailure",
                })
                fail_el.text = f"Action: {action}\nLocator: {locator}\nDetails:\n{detail}"
            elif step_status in ("error", "aborted", "timeout"):
                err_el = ET.SubElement(testcase, "error", {
                    "message": detail or f"Execution error ({step_status})",
                    "type": "ExecutionError",
                })
                err_el.text = f"Action: {action}\nLocator: {locator}\nStatus: {step_status}\nDetails:\n{detail}"

    output_path.parent.mkdir(parents=True, exist_ok=True)
    tree = ET.ElementTree(testsuites)
    ET.indent(tree, space="  ", level=0)
    tree.write(output_path, encoding="utf-8", xml_declaration=True)


def main():
    args = parse_args()
    out_dir = Path(args.out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    report_path = Path(args.report_path).resolve()

    run_id = uuid.uuid4().hex
    print(f"=== 1C:Specter CLI Runner ===")
    print(f"Run ID:        {run_id}")
    print(f"Target Module: {args.module or '<all workspace suites>'}")
    if args.test:
        print(f"Target Test:   {args.test}")
    print(f"Exchange Dir:  {out_dir}")
    print(f"Client Path:   {args.client_path}")
    print(f"Timeout:       {args.timeout}s")
    print(f"Report Output: {report_path}")
    print("---------------------------------------")

    commands_file = out_dir / "bridge-commands.json"
    result_file = out_dir / f"bridge-result-{run_id}.json"

    # Clean previous stale files if any
    for old_file in [commands_file, result_file]:
        if old_file.exists():
            try:
                old_file.unlink()
            except OSError:
                pass

    # Step 1: Write bridge-commands.json atomically
    command_payload = {
        "runId": run_id,
        "module": args.module,
        "test": args.test,
        "timestamp": int(time.time() * 1000),
        "commands": [],
    }
    
    print("[1/3] Writing bridge-commands.json atomically...")
    atomic_write_json(commands_file, command_payload)

    # Step 2: Launch 1C thin client
    cmd = build_1c_command(
        client_path=args.client_path,
        ib_conn=args.ib_conn,
        out_dir_str=str(out_dir),
        user=args.user,
        password=args.password,
    )

    print(f"[2/3] Spawning 1C client: {' '.join(cmd)}")
    start_time = time.time()
    proc = None
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except FileNotFoundError:
        err_msg = f"1C client executable not found at '{args.client_path}'"
        print(f"ERROR: {err_msg}", file=sys.stderr)
        generate_junit_xml(
            suite_name=args.module or "SpecterInit",
            run_id=run_id,
            status="error",
            steps=[],
            total_duration=0.0,
            output_path=report_path,
            error_message=err_msg,
        )
        return 1
    except Exception as e:
        err_msg = f"Failed to spawn 1C client process: {e}"
        print(f"ERROR: {err_msg}", file=sys.stderr)
        generate_junit_xml(
            suite_name=args.module or "SpecterInit",
            run_id=run_id,
            status="error",
            steps=[],
            total_duration=0.0,
            output_path=report_path,
            error_message=err_msg,
        )
        return 1

    # Step 3: Wait for bridge-result-<runId>.json with timeout
    print(f"[3/3] Waiting for result file '{result_file.name}'...")
    result_data = None
    deadline = start_time + args.timeout

    while time.time() < deadline:
        if result_file.exists():
            result_data = read_json_safe(result_file)
            if result_data is not None and result_data.get("runId") == run_id:
                break
        
        # Check if 1C process terminated prematurely without output
        retcode = proc.poll()
        if retcode is not None:
            # Process exited, wait 1 more second in case file was flushed right at exit
            time.sleep(1.0)
            if result_file.exists():
                result_data = read_json_safe(result_file)
            break

        time.sleep(0.5)

    duration = time.time() - start_time

    # Terminate 1C process if still running after completion or timeout
    if proc.poll() is None:
        try:
            proc.terminate()
            proc.wait(timeout=5)
        except Exception:
            try:
                proc.kill()
            except Exception:
                pass

    # Evaluate execution result
    if result_data is None:
        if duration >= args.timeout:
            err_msg = f"Timeout ({args.timeout}s) exceeded waiting for 1C test execution"
        else:
            err_msg = f"1C process terminated with exit code {proc.returncode} before writing result file"
        
        print(f"\n[FAILED] {err_msg}", file=sys.stderr)
        generate_junit_xml(
            suite_name=args.module or "SpecterExecution",
            run_id=run_id,
            status="failed",
            steps=[],
            total_duration=duration,
            output_path=report_path,
            error_message=err_msg,
        )
        return 1

    # Parse results
    status = str(result_data.get("status", "unknown")).lower()
    steps = result_data.get("steps", [])
    
    passed_count = sum(1 for s in steps if str(s.get("status", "")).lower() == "passed")
    failed_count = sum(1 for s in steps if str(s.get("status", "")).lower() in ("failed", "failure"))
    error_count = sum(1 for s in steps if str(s.get("status", "")).lower() in ("error", "aborted", "timeout"))

    print("\n---------------------------------------")
    print(f"Test Run Completed in {duration:.2f}s")
    print(f"Overall Status: {status.upper()}")
    print(f"Total Steps:    {len(steps)}")
    print(f"Passed:         {passed_count}")
    print(f"Failed:         {failed_count}")
    print(f"Errors:         {error_count}")
    print("---------------------------------------")

    # Generate JUnit XML
    generate_junit_xml(
        suite_name=args.module or "SpecterTestSuite",
        run_id=run_id,
        status=status,
        steps=steps,
        total_duration=duration,
        output_path=report_path,
    )
    print(f"JUnit XML report saved to: {report_path}")

    # Clean up exchange files
    if not args.keep_exchange:
        for f in [commands_file, result_file]:
            if f.exists():
                try:
                    f.unlink()
                except OSError:
                    pass

    return 0 if status == "passed" and failed_count == 0 and error_count == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
