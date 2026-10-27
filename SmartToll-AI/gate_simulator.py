
def simulate_gate(gate_decision):

    if gate_decision == "ALLOW":
        print("🟢 GATE OPEN")
        print("Vehicle can pass.")

    else:
        print("🔴 GATE CLOSED")
        print("Payment required.")


# Test the gate
simulate_gate("ALLOW")
simulate_gate("PAYMENT_REQUIRED")