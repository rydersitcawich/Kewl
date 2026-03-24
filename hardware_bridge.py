#!/usr/bin/env python3
"""
hardware_bridge.py — Minimal bridge: Java simulation -> Tektronix PWS4323.

Reads JSON lines from stdin, sets power supply voltage per epoch.
No sensor feedback yet (thermocouple/FLIR added later).

Protocol (one JSON line per epoch):
  Java -> Python:  {"epoch": N, "state": "sprinting"|"working"|"idle"}
  Python -> Java:  {"epoch": N, "ack": true}

Install:
  pip install pyvisa pyvisa-py pyusb

Usage:
  # Test without hardware:
  echo '{"epoch":1,"state":"sprinting"}' | python3 hardware_bridge.py --dry-run

  # With real power supply:
  python3 hardware_bridge.py --sprint-voltage 5.0 --work-voltage 2.5 --idle-voltage 0.0
"""

import sys
import json
import argparse
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    stream=sys.stderr,
)
log = logging.getLogger("bridge")


class PowerSupply:
    def __init__(self, resource_name=None, sprint_voltage=5.0,
                 work_voltage=2.5, idle_voltage=0.0, current_limit=1.0):
        self.sprint_voltage = sprint_voltage
        self.work_voltage = work_voltage
        self.idle_voltage = idle_voltage
        self.inst = None

        import pyvisa
        rm = pyvisa.ResourceManager("@py")
        resources = rm.list_resources()
        log.info(f"VISA resources found: {resources}")

        if resource_name is None:
            for r in resources:
                if "USB" in r:
                    resource_name = r
                    break

        if resource_name is None:
            raise RuntimeError("No USB VISA resource found. Is the PWS4323 connected?")

        self.inst = rm.open_resource(resource_name)
        self.inst.timeout = 5000

        idn = self.inst.query("*IDN?").strip()
        log.info(f"Connected to: {idn}")

        # Safety: set current limit, start at idle, enable output
        self.inst.write(f"SOUR:CURR {current_limit}")
        self.inst.write(f"SOUR:VOLT {self.idle_voltage}")
        self.inst.write("OUTP ON")
        log.info(f"Ready: sprint={sprint_voltage}V, work={work_voltage}V, idle={idle_voltage}V, Ilimit={current_limit}A")

    def set_state(self, state: str):
        voltage = {
            "sprinting": self.sprint_voltage,
            "working":   self.work_voltage,
            "idle":      self.idle_voltage,
        }.get(state, self.idle_voltage)
        self.inst.write(f"SOUR:VOLT {voltage}")
        log.info(f"VOLT -> {voltage}V (state={state})")

    def shutdown(self):
        if self.inst:
            self.inst.write("SOUR:VOLT 0")
            self.inst.write("OUTP OFF")
            self.inst.close()
            log.info("Power supply shut down.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true",
                        help="No hardware — just log what would happen")
    parser.add_argument("--sprint-voltage", type=float, default=12.0)
    parser.add_argument("--work-voltage", type=float, default=6.0)
    parser.add_argument("--idle-voltage", type=float, default=0.0)
    parser.add_argument("--current-limit", type=float, default=2.0)
    parser.add_argument("--visa-resource", type=str, default=None,
                        help="VISA resource string (auto-detect if omitted)")
    args = parser.parse_args()

    psu = None
    if not args.dry_run:
        psu = PowerSupply(
            resource_name=args.visa_resource,
            sprint_voltage=args.sprint_voltage,
            work_voltage=args.work_voltage,
            idle_voltage=args.idle_voltage,
            current_limit=args.current_limit,
        )
    else:
        log.info("=== DRY RUN (no hardware) ===")

    try:
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            try:
                cmd = json.loads(line)
            except json.JSONDecodeError as e:
                log.error(f"Bad JSON: {e}")
                continue

            epoch = cmd.get("epoch", -1)
            state = cmd.get("state", "idle")

            if psu:
                psu.set_state(state)
            else:
                log.info(f"[DRY] Epoch {epoch}: would set {state.upper()} voltage")

            print(json.dumps({"epoch": epoch, "ack": True}), flush=True)

    except KeyboardInterrupt:
        pass
    finally:
        if psu:
            psu.shutdown()


if __name__ == "__main__":
    main()