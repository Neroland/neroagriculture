# Resource Fragment

Resource Fragment is a component-backed item produced by mature resource crops. Each stack carries the
server-resolved material id and fragment family, so one registered item can represent the entire material
catalog without creating a registry entry per material.

Yield starts at the catalog entry's configured minimum, rises deterministically with successful harvests,
and stops at its configured maximum after the ramp length. The server yield multiplier is applied after
the bounded ramp. The resulting identity and quantity are created from the server catalog; unchecked item
components supplied by a client are never copied into harvest output.

Later fabrication stages consume Resource Fragment. In Stage 4 it is the safe, persistent output of the
resource-crop loop.
