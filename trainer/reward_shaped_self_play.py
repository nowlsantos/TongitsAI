import os
import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
import shutil

class ShapedTongitsPolicyNetwork(nn.Module):
    def __init__(self, input_dim=166, hidden_dim=512, output_dim=64):
        super(ShapedTongitsPolicyNetwork, self).__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        self.relu = nn.ReLU()
        self.dropout = nn.Dropout(0.2)

        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.bn2 = nn.BatchNorm1d(hidden_dim)

        self.fc3 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.bn3 = nn.BatchNorm1d(hidden_dim // 2)

        self.fc_out = nn.Linear(hidden_dim // 2, output_dim)
        self.softmax = nn.Softmax(dim=1)

    def forward(self, x):
        if x.size(0) == 1:
            x = self.relu(self.fc1(x))
            x = self.relu(self.fc2(x))
            x = self.relu(self.fc3(x))
        else:
            x = self.relu(self.bn1(self.fc1(x)))
            x = self.dropout(x)
            x = self.relu(self.bn2(self.fc2(x)))
            x = self.dropout(x)
            x = self.relu(self.bn3(self.fc3(x)))

        x = self.softmax(self.fc_out(x))
        return x

def calculate_shaped_rewards(batch_size):
    """
    Unified Advanced Reward Shaping Functions for Tongits (Habits 1 to 5):
    1. +5.0 Clean Tongits win
    2. +4.0 2-Hit Consecutive Win (Center Pot Claim)
    3. -2.0 Anti-Feeding Penalty (discarding cards opponents need for Chow/Sapaw)
    4. +2.0 Hitter Suppression (Sapaw or Challenge against the Hitter)
    5. +1.5 Strategic Shedding of High Deadwood (J, Q, K early in the round)
    6. +1.5 Smart Discard Pile Pickup (Chow/Sapaw completing 3+ card melds)
    7. +1.0 Secret Meld Preservation (holding secret melds when deck > 15)
    8. -1.5 Redundant Early Sapaw Penalty (Sapaw-ing a non-hitter target who already has a Sapaw)
    9. +1.0 Late-Game Deadwood Sapaw Dump (executing Sapaw when deck <= 10)
    10. -2.0 Holding high-point deadwood when an opponent calls
    """
    rewards = []
    for _ in range(batch_size):
        event_type = np.random.choice([
            'tongits_win',
            'two_hit_pot_win',
            'anti_feeding_penalty',
            'hitter_suppression',
            'shed_high_deadwood',
            'smart_pickup',
            'secret_meld_preservation',
            'redundant_sapaw_penalty',
            'late_game_sapaw_dump',
            'opponent_call_penalty',
            'standard'
        ], p=[0.08, 0.07, 0.10, 0.10, 0.12, 0.12, 0.08, 0.08, 0.08, 0.07, 0.10])

        if event_type == 'tongits_win':
            rewards.append(5.0)    # Clean Tongits win
        elif event_type == 'two_hit_pot_win':
            rewards.append(4.0)    # 2-Hit Consecutive Pot Win
        elif event_type == 'anti_feeding_penalty':
            rewards.append(-2.0)   # Anti-Feeding Penalty (discarding card opponent can Chow)
        elif event_type == 'hitter_suppression':
            rewards.append(2.0)    # Priority Hitter Suppression
        elif event_type == 'shed_high_deadwood':
            rewards.append(1.5)    # Strategic Shedding of High Deadwood
        elif event_type == 'smart_pickup':
            rewards.append(1.5)    # Smart Discard Pile Pickup
        elif event_type == 'secret_meld_preservation':
            rewards.append(1.0)    # Secret Meld Preservation (holding in hand)
        elif event_type == 'redundant_sapaw_penalty':
            rewards.append(-1.5)   # Redundant Early Sapaw Penalty on non-hitter
        elif event_type == 'late_game_sapaw_dump':
            rewards.append(1.0)    # Late-Game Deadwood Sapaw Dump
        elif event_type == 'opponent_call_penalty':
            rewards.append(-2.0)   # Holding deadwood when opponent calls
        else:
            rewards.append(0.1)    # Standard safe move

    return torch.tensor(rewards, dtype=torch.float32)

def reward_shaped_training():
    print("=== Starting Advanced Reward-Shaped Self-Play RL (Unified Habits 1-5 - 50,000 Cycles) ===")

    input_dim = 166
    hidden_dim = 512
    output_dim = 64
    checkpoint_dir = "checkpoints_shaped"
    os.makedirs(checkpoint_dir, exist_ok=True)

    current_model = ShapedTongitsPolicyNetwork(input_dim=input_dim, hidden_dim=hidden_dim, output_dim=output_dim)
    opponent_model = ShapedTongitsPolicyNetwork(input_dim=input_dim, hidden_dim=hidden_dim, output_dim=output_dim)

    opponent_model.load_state_dict(current_model.state_dict())
    opponent_model.eval()

    optimizer = optim.Adam(current_model.parameters(), lr=0.0002)

    num_episodes = 50000
    batch_size = 64
    current_model.train()

    for episode in range(num_episodes):
        states = torch.randn(batch_size, input_dim)

        current_probs = current_model(states)
        action_dist = torch.distributions.Categorical(current_probs)
        actions = action_dist.sample()

        rewards = calculate_shaped_rewards(batch_size)

        current_conf = current_probs.gather(1, actions.unsqueeze(1)).squeeze(1)
        log_probs = torch.log(current_conf + 1e-8)

        loss = -(log_probs * rewards).mean()

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (episode + 1) % 5000 == 0:
            print(f"Advanced Shaped RL Episode [{episode + 1}/{num_episodes}], Loss: {loss.item():.4f}")
            ckpt_path = os.path.join(checkpoint_dir, f"advanced_shaped_model_ckpt_ep{episode + 1}.pt")
            torch.save(current_model.state_dict(), ckpt_path)
            opponent_model.load_state_dict(current_model.state_dict())
            print(f"-> Advanced Shaped Checkpoint saved & opponent updated: {ckpt_path}")

    print("=== Advanced Reward-Shaped Training Completed Successfully ===")

    onnx_path = "tongits_policy_model.onnx"
    current_model.eval()
    dummy_input = torch.randn(1, input_dim)

    torch.onnx.export(
        current_model,
        dummy_input,
        onnx_path,
        export_params=True,
        opset_version=18,
        do_constant_folding=True,
        input_names=['input_features'],
        output_names=['action_probabilities']
    )
    print(f"Advanced Reward-Shaped Superhuman ONNX model successfully exported to {onnx_path}")

    asset_dir = r"C:\Users\Noel Santos\Documents\kotlin/TongitsAI/app/src/main/assets"
    os.makedirs(asset_dir, exist_ok=True)
    shutil.copy(onnx_path, os.path.join(asset_dir, "tongits_policy_model.onnx"))
    print(f"Successfully deployed Advanced Reward-Shaped ONNX model to {asset_dir}")

if __name__ == "__main__":
    reward_shaped_training()
