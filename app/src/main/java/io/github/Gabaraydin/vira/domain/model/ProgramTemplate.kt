package io.github.Gabaraydin.vira.domain.model

// Identifiers only, shown in the empty-state picker (issue #9) via string resources — the
// display name is a UI-layer concern, not a domain one. Loading a template's actual days
// and exercises into a new program is issue #10's job.
enum class ProgramTemplate {
    FIVE_DAY_SPLIT,
    PUSH_PULL_LEGS,
    PUSH_PULL_LEGS_DOUBLE,
    UPPER_LOWER,
    FULL_BODY_THREE_DAY,
    BRO_SPLIT,
}
