# Design for future flow modelling in Waltz

[TOC levels=2-3]:  # "# Contents"
# Contents
- [Status: DRAFT](#status-draft)
- [Terminology](#terminology)
- [Motivation](#motivation)
- [Proposal](#proposal)
- [Persistence](#persistence)
    - [Change Set table](#change-set-table)
    - [Change Unit table](#change-unit-table)
    - [Data flow tables](#data-flow-tables)
    - [Entity Lifecycle Status enum](#entity-lifecycle-status-enum)
    - [Impact / Risks](#impact--risks)
    - [Limitations](#limitations)
- [UI Requirements](#ui-requirements)
- [Possible future enhancements](#possible-future-enhancements)
- [Potential Reporting views](#potential-reporting-views)


## Status: DRAFT

This feature is currently in draft design stages intended to target the
1.17 release.


## Terminology

In the following section we will use the following terminology:

- **Change Unit**: represents unit of change, i.e. a flow is being
  retired
- **Change Set**: represents a group of change units to be executed
  together. The change set can be associated with a Change Initiative
  and has a start / end dates.
- **Change Initiative**: This is a representation of the over-arching
  change driver. A change initiative may be associated with several
  change sets, which may / may not be in-flight simultaneously.


## Motivation

Waltz presently captures current state architecture only and does not
model future state or allow the representation of planned changes. There
is a need to model future state architecture for data flows (initially)
to capture proposals and plans for creation or decommission of both
logical and physical flows.

The need is to be able to both model future states of application flows,
associate them to change initiatives and also record the correlation of
changes units with one another, i.e. a creation of a flow, results in
the decommission of another flow.


## Proposal

1. Capture proposed changes to logical data flows data flows, an
   effective date and the associated initiative driving the change.
2. As above for physical data flows data flows.
3. Provide the ability to plan a change into a data flows data types,
   i.e. Decommission the consumption of a specific data type from a
   logical flow (**TBC**)
4. View a summary of changes (as above) from the perspective of a change
   initiative / organisational unit
5. Provide the ability to view changes along with current state in
   exiting views.
   - Logical flows: Allow overlaying of future state on the existing
     current state views/visualisations i.e. Source Target graph, Flow
     Diagrams, Boingy Graph
   - Logical Flow Data Types: Same as above (Logical Flows)
   - Physical flows: Conditionally show / hide future flows in physical
     flow tables and also highlight on Flow Diagrams.


## Persistence


### Change Set table

Represents a collection of changes that are intended to be carried out
as an 'atomic' set.

| Column                    | Type      | Mandatory | Description                                                      |
|:--------------------------|:----------|:----------|:-----------------------------------------------------------------|
| `id`                      | long      | **y**     | id of this change set                                            |
| `parent_entity_kind`      | enum      | n         | the kind of the parent of this change set i.e. CHANGE_INITIATIVE |
| `parent_entity_id`        | long      | n         | the id of the parent of this change set                          |
| `start_date`              | date      | n         | the planned start date of this change set                        |
| `end_date`                | date      | n         | the planned end date of this change set                          |
| `entity_lifecycle_status` | enum      | **y**     | lifecycle status of this change set                              |
| `name`                    | string    | n         | name                                                             |
| `description`             | string    | n         | description                                                      |
| `last_updated_by`         | string    | **y**     | user id of last editor or creator if new                         |
| `last_updated_at`         | timestamp | **y**     | when this record was created                                     |
| `external_id`             | string    | n         | the external id of this change set (if applicable)               |
| `provenance`              | string    | **y**     | the provenance of the record                                     |

- Primary key on (`id`).
- Non unique index on (`parent_entity_id`, `parent_entity_kind`).


### Change Unit table

Represents an individual unit of change along with the entity reference
of the subject being changed.

| Column                   | Type      | Mandatory | Description                                                             |
|:-------------------------|:----------|:----------|:------------------------------------------------------------------------|
| `id`                     | long      | **y**     | id of this change unit                                                  |
| `change_set_id`          | long      | **y**     | id of the parent change set                                             |
| `subject_entity_kind`    | enum      | **y**     | the kind of the entity being effected by this change                    |
| `subject_entity_id`      | long      | **y**     | the id of the entity being effected by this change                      |
| `subject_initial_status` | enum      | **y**     | initial entity lifecycle status of the subject                          |
| `action`                 | enum      | **y**     | the change action taking place: `ACTIVATE`, `RETIRE`                    |
| `execution_status`       | enum      | **y**     | execution state of this change unit: `PENDING`, `COMPLETE`, `DISCARDED` |
| `name`                   | string    | n         | name                                                                    |
| `description`            | string    | n         | description                                                             |
| `last_updated_by`        | string    | **y**     | user id of last editor or creator if new                                |
| `last_updated_at`        | timestamp | **y**     | when this record was created                                            |
| `external_id`            | string    | n         | the external id of this change set (if applicable)                      |
| `provenance`             | string    | **y**     | the provenance of the record                                            |

- Primary key on (`id`)
- Non unique index on (`change_set_id`)
- Non unique index on (`subject_entity_id`, `subject_entity_kind`)

### Data flow tables

- Add `is_removed` to `logical_flow` to deal with soft record deletion
- Add `entity_lifecycle_status` to `physical_flow` to support changes

### Entity Lifecycle Status enum

Add the values: `DRAFT` and `RETIRING` resulting in the enum:

- **DRAFT** (New)
- PENDING
- ACTIVE
- **RETIRING** (New)
- REMOVED


### Impact / Risks

The change to the `logical_flow` table will impact any code that relies
on `entity_lifecycle_status` being a definition of the records
'liveness' status. This will need to be refactored to consider the
`is_removed` column.

Outside of the Waltz core any reporting that is driven off the above
table will be effected, including entity statistics or management
reports driven by these tables.


### Limitations

This proposal does not deal with modifications to a subjects attributes
i.e. A Physical Flow's transport, frequency or criticality. This will
only deal with activation and decommissioning of subjects.


## UI Requirements

- Ability to view / create change sets and add change units to them
  - View a list of change sets applicable to an org unit / change
    initiative. Should allow drill down to view individual changes
    composing the change set.
  - Ability to create a change set, assign to a CI.
  - Ability to assign logical / physical flow changes to change set
    potentially using an inline panel editor.

- Source target graph
  - show future flows as optional overlay with ability to filter change
    sets
  - provide inline editing support of future flows i.e. Quick decom and
    link to change set

- Flow diagram
  - Provide the ability to include future flows into curated flow
    diagram
  - Include quick filters to show / hide future flows from flow diagram
    (already partially complete in an earlier form with PENDING /
    REMOVED flows)

- Boingy Graphy: Optionally show / hide future flows from visualisation


See [Future Flow mockups](TBD)


## Possible future enhancements

Providing the ability to better model subject attribute changes.


## Potential Reporting views

TBD
