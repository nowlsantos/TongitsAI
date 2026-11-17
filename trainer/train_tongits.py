import os
import json
import torch
import torch.nn as nn
import torch.optim as optim

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

def train_and_export():
    print("TongitsAI Training on Real Minimax Dataset & ONNX Export")

    dataset_path = "training_dataset.json"
    if not os.path.exists(dataset_path):
        print(f"Error: {dataset_path} not found. Please generate training_dataset.json first.")
        return

    with open(dataset_path, "r") as f:
        data = json.load(f)

    if not data:
        print("Dataset is empty!")
        return

    features = [torch.tensor(sample["features"], dtype=torch.float32) for sample in data]
    actions = [torch.tensor(abs(sample["actionHash"]) % 64, dtype=torch.long) for sample in data]

    X_train = torch.stack(features)
    y_train = torch.stack(actions)

    print(f"Loaded {len(X_train)} training samples.")

    model = TongitsPolicyNetwork()
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    criterion = nn.CrossEntropyLoss()

    epochs = 20
    model.train()

    for epoch in range(epochs):
        optimizer.zero_grad()
        outputs = model(X_train)
        loss = criterion(outputs, y_train)
        loss.backward()
        optimizer.step()
        print(f"Epoch [{epoch + 1}/{epochs}], Loss: {loss.item():.4f}")

    print("=== Training Completed Successfully ===")

    onnx_path = "tongits_policy_model.onnx"
    model.eval()
    dummy_input = torch.randn(1, 166)

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
    print(f"Trained model successfully exported to {onnx_path}")

if __name__ == "__main__":
    train_and_export()
