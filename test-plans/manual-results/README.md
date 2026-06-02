# Manual Results Artifacts

This directory stores scenario evidence captured during Copilot-driven runs.

Expected structure after running the JBang script:

- `<scenario>/<mode>/before-<timestamp>.json`
- `<scenario>/<mode>/after-<timestamp>.json`
- `<scenario>/<mode>/delta-<timestamp>.json`
- `<scenario>/<mode>/delta-<timestamp>.csv`
- `metrics-deltas.csv` (aggregate for all scenarios and modes)

Capture commands:

```powershell
jbang scripts/actuator-delta.java start --scenario S1 --mode conventional --base-url http://localhost:8080
# run the scenario
jbang scripts/actuator-delta.java finish --scenario S1 --mode conventional --base-url http://localhost:8080
```
