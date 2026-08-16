# MoreGrid

A NeoForge 1.21.1 circuit-board component add-on for Create: Power Grid.

Adds four rotatable parts for circuit boards:

- **Transformer** — 4-terminal, configurable winding ratio (2–30 total turns), lumped equivalent circuit with realistic losses

<img width="519" height="483" alt="Transformer dropping from 220 to 12 at a button control panel" src="https://github.com/user-attachments/assets/d36ee266-355d-4fbd-8600-bd7c999bd684" />

- **Cartridge fuse** — one-shot delayed I²t fuse (0.25–32 A), blows permanently and requires full replacement

<img width="401" height="374" alt="Fuse in a radar motor control panel" src="https://github.com/user-attachments/assets/116e673e-b32d-471f-95dc-b31a9416f9d6" />

- **SCR thyristor** — latching switch with gate trigger, holding current, and forward drop

- **Zinc-carbon dry cell** — 1–12 series cells, depleting voltage/rising internal resistance, non-rechargeable

<img width="250" height="389" alt="Dry cell battery unit at the top" src="https://github.com/user-attachments/assets/9e389fc4-4d4c-4cd2-86d1-8071199689bb" />


Very WIP! Models, textures, behaviours are subject to change.

## Requirements

- Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- Create 6.0.10
- Create: Power Grid 0.5.5+

## Setup

Place the Power Grid JAR at `libs/powergrid-mc1.21.1-0.6.0.1.jar`, open in IntelliJ, and run the `client` configuration.
