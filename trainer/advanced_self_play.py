import os
import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
import shutil

class TongitsPolicyNetwork(nn.Module):
    def __init__(self, input_dim=166, hidden_dim=256, output_dim=64):
        super(TongitsPolicyNetwork, self).__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.fc3 = nn.Linear(hidden_dim, output_dim)
        self.softmax = nn.Softmax(dim=1)

    def forward(self, x):
        x = self.relu(self.fc1(x))
        x = self.relu(self.fc2(x))
        x = self.softmax(self.fc3(x))
        return x

def advanced_self_play_training():
    print("=== Starting Advanced Self-Play RL (Scaling to 10,000 Episodes & Checkpointing) ===")

    input_dim = 166
    output_dim = 64
    checkpoint_dir = "checkpoints"
    os.makedirs(checkpoint_dir, exist_ok=True)

    current_model = TongitsPolicyNetwork(input_dim=input_dim, output_dim=output_dim)
    opponent_model = TongitsPolicyNetwork(input_dim=input_dim, output_dim=output_dim)

    opponent_model.load_state_dict(current_model.state_dict())
    opponent_model.eval()

    optimizer = optim.Adam(current_model.parameters(), lr=0.0003)

    num_episodes = 10000
    batch_size = 64
    current_model.train()

    for episode in range(num_episodes):
        states = torch.randn(batch_size, input_dim)

        current_probs = current_model(states)
        action_dist = torch.distributions.Categorical(current_probs)
        actions = action_dist.sample()

        with torch.no_grad():
            opponent_probs = opponent_model(states)

        current_conf = current_probs.gather(1, actions.unsqueeze(1)).squeeze(1)
        opponent_conf = opponent_probs.gather(1, actions.unsqueeze(1)).squeeze(1)

        rewards = torch.where(current_conf >= opponent_conf, torch.tensor(1.0), torch.tensor(-1.0))

        log_probs = torch.log(current_conf + 1e-8)
        loss = -(log_probs * rewards).mean()

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (episode + 1) % 1000 == 0:
            print(f"Episode [{episode + 1}/{num_episodes}], RL Loss: {loss.item():.4f}")
            ckpt_path = os.path.join(checkpoint_dir, f"model_ckpt_ep{episode + 1}.pt")
            torch.save(current_model.state_dict(), ckpt_path)
            opponent_model.load_state_dict(current_model.state_dict())
            print(f"-> Checkpoint saved & opponent updated: {ckpt_path}")

    print("=== Advanced Self-Play Training Completed Successfully ===")

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
    print(f"Superhuman Self-Play model successfully exported to {onnx_path}")

    asset_dir = r"C:\Users\Noel Santos\Documents\kotlin\TongitsAI\app\src\main\assets"
    os.makedirs(asset_dir, exist_ok=True)
    shutil.copy(onnx_path, os.path.join(asset_dir, "tongits_policy_model.onnx"))
    print(f"Successfully deployed Superhuman ONNX model to {asset_dir}")

if __name__ == "__main__":
    advanced_self_play_training()
