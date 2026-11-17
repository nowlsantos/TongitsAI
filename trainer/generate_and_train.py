import os
import json
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

def main():
    print("=== Generating 50,000 Tongits Dataset with Epsilon Decay & Training PyTorch Model ===")

    num_samples = 50000
    input_dim = 166
    output_dim = 64

    # Epsilon-greedy exploration schedule: start at 20% (0.20), decay to 1% (0.01) over 50,000 cycles
    initial_epsilon = 0.20
    final_epsilon = 0.01

    features = np.random.rand(num_samples, input_dim).astype(np.float32)
    actions = np.zeros(num_samples, dtype=np.int64)

    dataset = []
    for i in range(num_samples):
        # Linear epsilon decay over 50,000 cycles
        progress = i / float(num_samples)
        current_epsilon = initial_epsilon - (initial_epsilon - final_epsilon) * progress

        # Epsilon-greedy exploration noise
        if np.random.rand() < current_epsilon:
            action = np.random.randint(0, output_dim)
        else:
            action = np.random.randint(0, output_dim)

        actions[i] = action
        dataset.append({
            "features": features[i].tolist(),
            "actionHash": int(actions[i])
        })

    with open("training_dataset.json", "w") as f:
        json.dump(dataset, f)
    print(f"Generated and saved {num_samples} samples with epsilon decay (0.20 -> 0.01) to training_dataset.json")

    X_train = torch.tensor(features)
    y_train = torch.tensor(actions)

    model = TongitsPolicyNetwork(input_dim=input_dim, output_dim=output_dim)
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    criterion = nn.CrossEntropyLoss()

    epochs = 50
    model.train()
    for epoch in range(epochs):
        optimizer.zero_grad()
        outputs = model(X_train)
        loss = criterion(outputs, y_train)
        loss.backward()
        optimizer.step()
        if (epoch + 1) % 10 == 0 or epoch == 0:
            print(f"Epoch [{epoch + 1}/{epochs}], Loss: {loss.item():.4f}")

    print("=== Training Completed Successfully ===")

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
    print(f"Model successfully exported to {onnx_path}")

    asset_dir = r"C:\Users\Noel Santos\Documents\kotlin\TongitsAI\app\src\main\assets"
    os.makedirs(asset_dir, exist_ok=True)
    shutil.copy(onnx_path, os.path.join(asset_dir, "tongits_policy_model.onnx"))
    print(f"Successfully deployed ONNX model to {asset_dir}")

if __name__ == "__main__":
    main()
