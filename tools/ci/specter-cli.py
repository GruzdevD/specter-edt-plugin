#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
1C:Specter Parallel Headless CLI Runner & Test Sharding Tool
Executes 1C:Specter UI tests in parallel using isolated temporary directories
and generates a consolidated JUnit XML report for CI/CD pipelines.
"""

import argparse
import concurrent.futures
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser(
        description="1C:Specter Parallel CLI Test Runner (CI/CD Sharding)",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--out-dir",
        default="",
        help="Optional fallback directory for exchange files (if not using tempdirs)",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=1,
        help="Number of parallel worker processes for test sharding",
    )
    parser.add_argument(
        "--test-list",
        default="",
        help="Path to JSON file containing a list of test modules or test cases to execute",
    )
    parser.add_argument(
        "--module",
        default="",
        help="Target single test suite module name (if --test-list is not provided)",
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
        help="Maximum timeout in seconds per worker to wait for test run completion",
    )
    parser.add_argument(
        "--report-path",
        default="junit-report.xml",
        help="Path where final consolidated JUnit XML report will be generated",
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

    tmp_path.replace(file_path)


def read_json_safe(file_path: Path, max_attempts=10, delay=0.1):
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

    conn_str = ib_conn.strip()
    if conn_str.startswith("/F ") or conn_str.startswith("/S ") or conn_str.startswith("/IBConnectionString "):
        cmd.extend(conn_str.split(" ", 1))
    elif conn_str.startswith("File=") or conn_str.startswith("Srvr="):
        cmd.extend(["/IBConnectionString", conn_str])
    else:
        cmd.extend(["/IBConnectionString", f'File="{conn_str}"'])

    if user:
        cmd.extend(["/N", user])
    if password:
        cmd.extend(["/P", password])

    cmd.extend(["/C", f"SPECTER_START_BRIDGE|outDir={out_dir_str}"])
    return cmd


def run_worker(worker_id: int, module_name: str, test_name: str, client_path: str, ib_conn: str, timeout: int, user: str, password: str):
    """
    Executes a single test module/case in an isolated temporary directory.
    Returns a dict with execution results, steps, status, duration, and worker info.
    """
    run_id = uuid.uuid4().hex
    start_time = time.time()
    
    # Create isolated temporary directory for file exchange
    temp_dir = tempfile.TemporaryDirectory(prefix=f"specter_worker_{worker_id}_")
    temp_path = Path(temp_dir.name).resolve()

    commands_file = temp_path / "bridge-commands.json"
    result_file = temp_path / f"bridge-result-{run_id}.json"

    print(f"[Worker {worker_id}] Starting module '{module_name or 'ALL'}' (runId: {run_id[:8]}...) in {temp_path.name}")

    command_payload = {
        "runId": run_id,
        "module": module_name,
        "test": test_name,
        "timestamp": int(time.time() * 1000),
        "commands": [],
    }

    try:
        atomic_write_json(commands_file, command_payload)

        cmd = build_1c_command(
            client_path=client_path,
            ib_conn=ib_conn,
            out_dir_str=str(temp_path),
            user=user,
            password=password,
        )

        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        deadline = start_time + timeout
        result_data = None

        while time.time() < deadline:
            if result_file.exists():
                result_data = read_json_safe(result_file)
                if result_data is not None and result_data.get("runId") == run_id:
                    break

            retcode = proc.poll()
            if retcode is not None:
                time.sleep(1.0)
                if result_file.exists():
                    result_data = read_json_safe(result_file)
                break

            time.sleep(0.5)

        duration = time.time() - start_time

        if proc.poll() is None:
            try:
                proc.terminate()
                proc.wait(timeout=3)
            except Exception:
                try:
                    proc.kill()
                except Exception:
                    pass

        if result_data is None:
            err_msg = f"Worker {worker_id} timeout or 1C crash (exit code: {proc.returncode})"
            print(f"[Worker {worker_id}] ERROR: {err_msg}")
            return {
                "worker_id": worker_id,
                "module": module_name or "DefaultModule",
                "run_id": run_id,
                "status": "failed",
                "duration": duration,
                "steps": [{
                    "id": 1,
                    "action": f"WorkerRun_{module_name or 'All'}",
                    "status": "failed",
                    "message": err_msg,
                    "durationMs": int(duration * 1000),
                }],
                "error": err_msg,
            }

        status = str(result_data.get("status", "unknown")).lower()
        steps = result_data.get("steps", [])
        print(f"[Worker {worker_id}] Finished module '{module_name or 'ALL'}' with status: {status.upper()} in {duration:.2f}s")

        return {
            "worker_id": worker_id,
            "module": module_name or "DefaultModule",
            "run_id": run_id,
            "status": status,
            "duration": duration,
            "steps": steps,
            "error": "",
        }

    except Exception as e:
        duration = time.time() - start_time
        err_msg = f"Worker {worker_id} exception: {e}"
        print(f"[Worker {worker_id}] ERROR: {err_msg}")
        return {
            "worker_id": worker_id,
            "module": module_name or "DefaultModule",
            "run_id": run_id,
            "status": "error",
            "duration": duration,
            "steps": [{
                "id": 1,
                "action": f"WorkerException_{module_name or 'All'}",
                "status": "error",
                "message": err_msg,
                "durationMs": int(duration * 1000),
            }],
            "error": err_msg,
        }
    finally:
        try:
            temp_dir.cleanup()
        except Exception:
            pass


def generate_consolidated_junit(results: list, report_path: Path):
    """Generates a consolidated JUnit XML report from all worker results."""
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_time = 0.0

    for res in results:
        total_time += res.get("duration", 0.0)
        steps = res.get("steps", [])
        if not steps:
            total_tests += 1
            if res.get("status") != "passed":
                total_failures += 1
        else:
            for s in steps:
                total_tests += 1
                st = str(s.get("status", "")).lower()
                if st in ("failed", "failure"):
                    total_failures += 1
                elif st in ("error", "aborted", "timeout"):
                    total_errors += 1

    testsuites = ET.Element("testsuites", {
        "name": "Specter Parallel 1C UI Tests",
        "tests": str(total_tests),
        "failures": str(total_failures),
        "errors": str(total_errors),
        "time": f"{total_time:.3f}",
    })

    # Group by test suite / module
    modules_map = {}
    for res in results:
        mod = res.get("module", "DefaultSuite")
        if mod not in modules_map:
            modules_map.append(mod) if mod not in modules_map else None
            # actually store in dict
        if mod not in modules_map:
            modules_map[mod] = []
        modules_map[mod].append(res)

    for mod_name, mod_results in modules_map.items():
        mod_tests = 0
        mod_failures = 0
        mod_errors = 0
        mod_time = 0.0

        for r in mod_results:
            mod_time += r.get("duration", 0.0)
            steps = r.get("steps", [])
            if not steps:
                mod_tests += 1
                if r.get("status") != "passed":
                    mod_failures += 1
            else:
                for s in steps:
                    mod_tests += 1
                    st = str(s.get("status", "")).lower()
                    if st in ("failed", "failure"):
                        mod_failures += 1
                    elif st in ("error", "aborted", "timeout"):
                        mod_errors += 1

        testsuite = ET.SubElement(testsuites, "testsuite", {
            "name": mod_name,
            "tests": str(mod_tests),
            "failures": str(mod_failures),
            "errors": str(mod_errors),
            "time": f"{mod_time:.3f}",
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()),
        })

        for r in mod_results:
            steps = r.get("steps", [])
            run_id = r.get("run_id", "")
            if not steps:
                tc_name = f"{mod_name}_Run"
                testcase = ET.SubElement(testsuite, "testcase", {
                    "name": tc_name,
                    "classname": f"1C.{mod_name}",
                    "time": f"{r.get('duration', 0.0):.3f}",
                })
                if r.get("status") != "passed":
                    err_msg = r.get("error") or f"Execution status: {r.get('status')}"
                    fail_el = ET.SubElement(testcase, "failure", {
                        "message": err_msg,
                        "type": "SpecterExecutionFailure",
                    })
                    fail_el.text = err_msg
            else:
                for idx, s in enumerate(steps, 1):
                    action = s.get("action", f"step_{idx}")
                    locator = s.get("locator", "")
                    tc_name = locator if locator else (f"{action} [{s.get('id', idx)}]" if s.get("id") else action)
                    st = str(s.get("status", "")).lower()
                    detail = s.get("detail") or s.get("message") or s.get("error") or ""
                    step_time = float(s.get("durationMs", 0)) / 1000.0

                    testcase = ET.SubElement(testsuite, "testcase", {
                        "name": tc_name,
                        "classname": f"1C.{mod_name}",
                        "time": f"{step_time:.3f}",
                    })

                    if st in ("failed", "failure"):
                        fail_el = ET.SubElement(testcase, "failure", {
                            "message": detail or "Step failed",
                            "type": "AssertionFailure",
                        })
                        fail_el.text = f"Module: {mod_name}\nAction: {action}\nLocator: {locator}\nDetails:\n{detail}"
                    elif st in ("error", "aborted", "timeout"):
                        err_el = ET.SubElement(testcase, "error", {
                            "message": detail or f"Execution error ({st})",
                            "type": "ExecutionError",
                        })
                        err_el.text = f"Module: {mod_name}\nAction: {action}\nLocator: {locator}\nStatus: {st}\nDetails:\n{detail}"
                    elif st == "skipped":
                        skip_el = ET.SubElement(testcase, "skipped", {
                            "message": detail or "Test skipped",
                        })
                        skip_el.text = detail

    report_path.parent.mkdir(parents=True, exist_ok=True)
    tree = ET.ElementTree(testsuites)
    ET.indent(tree, space="  ", level=0)
    tree.write(report_path, encoding="utf-8", xml_declaration=True)


def main():
    args = parse_args()
    report_path = Path(args.report_path).resolve()

    print("========================================")
    print(" 1C:Specter Parallel CLI Test Runner  ")
    print("========================================")
    print(f"Workers:       {args.workers}")
    print(f"Client Path:   {args.client_path}")
    print(f"Timeout:       {args.timeout}s")
    print(f"Report Output: {report_path}")
    print("----------------------------------------")

    # Determine list of test modules/suites to execute
    tasks = []
    if args.test_list and Path(args.test_list).exists():
        try:
            with open(args.test_list, "r", encoding="utf-8") as f:
                data = json.load(f)
                if isinstance(data, list):
                    for item in data:
                        if isinstance(item, str):
                            tasks.append((item, args.test))
                        elif isinstance(item, dict):
                            tasks.append((item.get("module", ""), item.get("test", args.test)))
                elif isinstance(data, dict) and "modules" in data:
                    for mod in data["modules"]:
                        tasks.append((mod, args.test))
        except Exception as e:
            print(f"WARNING: Failed to parse test list file '{args.test_list}': {e}", file=sys.stderr)

    if not tasks:
        # Fallback to single module or empty (all suites)
        tasks.append((args.module, args.test))

    print(f"Total test execution tasks scheduled: {len(tasks)}")

    results = []
    max_workers = max(1, min(args.workers, len(tasks)))

    start_time = time.time()

    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_task = {}
        for idx, (mod_name, test_name) in enumerate(tasks, 1):
            future = executor.submit(
                run_worker,
                worker_id=idx,
                module_name=mod_name,
                test_name=test_name,
                client_path=args.client_path,
                ib_conn=args.ib_conn,
                timeout=args.timeout,
                user=args.user,
                password=args.password,
            )
            future_to_task[future] = (idx, mod_name)

        for future in concurrent.futures.as_completed(future_to_task):
            idx, mod_name = future_to_task[future]
            try:
                res = future.result()
                results.append(res)
            except Exception as exc:
                print(f"[Task {idx}] Module '{mod_name}' generated an exception: {exc}", file=sys.stderr)
                results.append({
                    "worker_id": idx,
                    "module": mod_name or "DefaultModule",
                    "run_id": "exception",
                    "status": "error",
                    "duration": 0.0,
                    "steps": [],
                    "error": str(exc),
                })

    total_duration = time.time() - start_time

    # Generate consolidated JUnit XML
    generate_consolidated_junit(results, report_path)

    # Calculate overall success
    failed_runs = 0
    total_steps_passed = 0
    total_steps_failed = 0

    for r in results:
        st = r.get("status", "").lower()
        if st != "passed":
            failed_runs += 1
        for s in r.get("steps", []):
            sst = str(s.get("status", "")).lower()
            if sst == "passed":
                total_steps_passed += 1
            elif sst in ("failed", "failure", "error", "aborted", "timeout"):
                total_steps_failed += 1

    print("\n========================================")
    print(f" Parallel Execution Summary              ")
    print("========================================")
    print(f"Total Duration:  {total_duration:.2f}s")
    print(f"Completed Tasks: {len(results)}")
    print(f"Failed Tasks:    {failed_runs}")
    print(f"Steps Passed:    {total_steps_passed}")
    print(f"Steps Failed:    {total_steps_failed}")
    print(f"JUnit Report:    {report_path}")
    print("========================================")

    return 0 if failed_runs == 0 and total_steps_failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
