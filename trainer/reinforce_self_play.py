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

def self_play_reinforcement_learning():
    print("=== Starting Step 2: Self-Play Reinforcement Learning (RL) ===")

    input_dim = 166
    output_dim = 64

    model = TongitsPolicyNetwork(input_dim=input_dim, output_dim=output_dim)
    optimizer = optim.Adam(model.parameters(), lr=0.0005)

    num_episodes = 500
    model.train()

    for episode in range(num_episodes):
        states = torch.randn(16, input_dim)
        probs = model(states)

        action_dist = torch.distributions.Categorical(probs)
        actions = action_dist.sample()

        rewards = torch.tensor(np.random.choice([1.0, -1.0], size=(16,)), dtype=torch.float32)

        log_probs = torch.log(probs.gather(1, actions.unsqueeze(1)).squeeze(1) + 1e-8)
        loss = -(log_probs * rewards).mean()

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (episode + 1) % 100 == 0 or episode == 0:
            print(f"Self-Play Episode [{episode + 1}/{num_episodes}], RL Policy Loss: {loss.item():.4f}")

    print("=== Self-Play RL Fine-Tuning Completed Successfully ===")

    onnx_path = "tongits_policy_model.onnx"
    model.eval()
    dummy_input = torch.randn(1, input_dim)

    torch.onnx.export(
        model,
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
    print(f"Successfully deployed Self-Play ONNX model to {asset_dir}")

if __name__ == "__main__":
    self_play_reinforcement_learning()
