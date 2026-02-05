# Particles Simulation

This project implements a simulation of charged particles moving inside a bounded 2D rectangle. The particles interact with each other through forces based on their electric charges and distances, and the simulation can be executed in sequential, parallel, and distributed modes.

---

## Overview

The simulation consists of **n particles**, each with:
- A position in a 2D plane
- A velocity
- A charge (positive or negative)

Particles move according to forces exerted by other particles and by the boundaries of the simulation area.

---

## Physics Model

For every pair of particles \( i \) and \( j \), the force magnitude is calculated as:

F = |c_i * c_j| / d^2

Where:
- `c_i`, `c_j` are the charges of the particles
- `d` is the Euclidean distance between them

### Interaction Rules
- Particles with the **same charge attract**
- Particles with **opposite charges repel**
- The simulation area is a rectangle whose boundaries repel particles to keep them inside

---

## Graphical Interface

The program includes an optional graphical visualization with the following requirements:

- Rendering can be **enabled or disabled**
- Graphics must run **independently of computation**
- Default window size: **800 × 600**
- Maximum rendering speed: **60 FPS**

---

## Execution Modes

The simulation must support three execution modes:

- **Sequential**
- **Parallel**
- **Distributed**

All modes must:
- Use the **same initial random particle configuration**
- Allow setting:
  - Number of particles
  - Number of simulation cycles
- Measure and report total execution time

---

## Performance Requirements

- Initial particle positions and charges must be generated using a **fixed random seed** when comparing runs
- The program should automatically adapt to the **available hardware resources**

---

## Testing and Evaluation

The project must include performance testing and analysis for all execution modes.

### Test 1: Varying Number of Particles
- Fix the number of cycles (e.g. 500)
- Increase the number of particles (e.g. 3000, 3500, 4000, ...)
- Measure execution time

### Test 2: Varying Number of Cycles
- Fix the number of particles (e.g. 500)
- Increase the number of cycles (e.g. 10,000, 15,000, ...)
- Measure execution time

Results should be presented using graphs and accompanied by analysis and discussion.

---

## Deliverables

- Source code for:
  - Sequential version
  - Parallel version
  - Distributed version
- Performance measurements
- Graphs and visual results
- Written analysis comparing implementations

---

## Notes

- Disabling graphics is recommended during performance testing
- Ensure proper synchronization in parallel and distributed implementations
- Use consistent parameters to ensure fair comparisons
