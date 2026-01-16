import SwiftUI
import ARKit
import SceneKit

struct ARContainer: UIViewRepresentable {
    @Binding var frames: [UIImage]

    func makeUIView(context: Context) -> ARSCNView {
        let arView = ARSCNView()
        let config = ARWorldTrackingConfiguration()
        config.planeDetection = .horizontal
        arView.session.run(config)
        
        arView.delegate = context.coordinator
        context.coordinator.sceneView = arView
        
        // Add Drag Gesture to move the object
        let panGesture = UIPanGestureRecognizer(target: context.coordinator, action: #selector(context.coordinator.handlePan(_:)))
        arView.addGestureRecognizer(panGesture)
        
        context.coordinator.startFallbackTimer()
        return arView
    }

    func updateUIView(_ uiView: ARSCNView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, ARSCNViewDelegate {
        var parent: ARContainer
        var sceneView: ARSCNView?
        var houseNode: SCNNode? // Store reference to our drawing
        var isPlaced = false
        
        init(_ parent: ARContainer) {
            self.parent = parent
        }

        func startFallbackTimer() {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                if !self.isPlaced { self.placeInAir() }
            }
        }

        func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
            guard !isPlaced, anchor is ARPlaneAnchor else { return }
            isPlaced = true
            addHouse(to: node)
        }

        func placeInAir() {
            guard !isPlaced, let sceneView = sceneView else { return }
            isPlaced = true
            let node = SCNNode()
            var translation = matrix_identity_float4x4
            translation.columns.3.z = -0.5 // 50cm away
            if let cameraTransform = sceneView.session.currentFrame?.camera.transform {
                node.simdTransform = matrix_multiply(cameraTransform, translation)
            }
            sceneView.scene.rootNode.addChildNode(node)
            addHouse(to: node)
        }

        func addHouse(to rootNode: SCNNode) {
            // 1. Swap Plane for a Box so it has "Depth" (0.01m = 1cm thick)
            // Increased size to 0.4 (40cm) so it's easier to see on 12 Mini
            let box = SCNBox(width: 0.4, height: 0.4, length: 0.01, chamferRadius: 0)
            let node = SCNNode(geometry: box)
            
            // 2. Fix Rotation: Make it stand upright and face the user
            node.eulerAngles.x = 0          // Keeps it vertical
            node.eulerAngles.z = .pi / 2    // Rotates it 90 degrees counter-clockwise to stand it up
            
            rootNode.addChildNode(node)
            self.houseNode = node
            animate(node: node)
        }

        // 3. Drag Logic: Moves the object where you touch the screen
        @objc func handlePan(_ gesture: UIPanGestureRecognizer) {
            guard let sceneView = sceneView, let node = houseNode else { return }
            let location = gesture.location(in: sceneView)
            
            // Raycast to find where the touch hits in 3D space
            if let query = sceneView.raycastQuery(from: location, allowing: .estimatedPlane, alignment: .any) {
                let results = sceneView.session.raycast(query)
                if let firstResult = results.first {
                    node.simdWorldTransform = firstResult.worldTransform
                }
            }
        }

        func animate(node: SCNNode) {
            var index = 0
            Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
                if !self.parent.frames.isEmpty {
                    index = (index + 1) % self.parent.frames.count
                    // Apply frame to front, back, and sides of the "box"
                    node.geometry?.firstMaterial?.diffuse.contents = self.parent.frames[index]
                }
            }
        }
    }
}
