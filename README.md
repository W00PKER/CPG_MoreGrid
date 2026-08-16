# MoreGrid

A NeoForge 1.21.1 circuit-board component add-on for Create: Power Grid.

Adds four rotatable parts for circuit boards:

- **Transformer** — 4-terminal, configurable winding ratio (2–30 total turns), lumped equivalent circuit with realistic losses

<img width="779" height="724" alt="{373F264A-E0EB-43BA-852A-2FFE9210976F}" src="https://github.com/user-attachments/assets/d36ee266-355d-4fbd-8600-bd7c999bd684" />

- **Cartridge fuse** — one-shot delayed I²t fuse (0.25–32 A), blows permanently and requires full replacement

<img width="602" height="561" alt="{E627A3F4-F1C3-4B10-9E88-CC994D90EF75}" src="https://github.com/user-attachments/assets/116e673e-b32d-471f-95dc-b31a9416f9d6" />

- **SCR thyristor** — latching switch with gate trigger, holding current, and forward drop

- **Zinc-carbon dry cell** — 1–12 series cells, depleting voltage/rising internal resistance, non-rechargeable

<img width="375" height="583" alt="{A8BD97ED-02B1-416F-9DD6-19C1ECEFCA66}" src="https://github.com/user-attachments/assets/9e389fc4-4d4c-4cd2-86d1-8071199689bb" />


Very WIP! Models, textures, behaviours are subject to change.

## Requirements

- Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- Create 6.0.10
- Create: Power Grid 0.5.5+

## Setup

Place the Power Grid JAR at `libs/powergrid-mc1.21.1-0.6.0.1.jar`, open in IntelliJ, and run the `client` configuration.
