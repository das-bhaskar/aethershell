import SwiftUI
import AVFoundation

struct ConnectView: View {
    // We only store the 'middle part' in this state variable
    @State private var tunnelID: String = ""
    @State private var sessionID: String = ""
    @State private var isConnected: Bool = false
    @State private var isSearching: Bool = false
    @State private var isShowingScanner = false
    @State private var errorMessage: String?

    // Computed property to build the final URL
    private var fullBaseURL: String {
        let cleaned = tunnelID.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if cleaned.isEmpty { return "" }
        return "https://\(cleaned).trycloudflare.com"
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 25) {
                Spacer().frame(height: 20)
                
                VStack(spacing: 8) {
                    Text("AetherShell")
                        .font(.system(size: 40, weight: .black, design: .rounded))
                    Text("Enter Tunnel ID to Connect")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Tunnel ID")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                        .padding(.leading, 5)
                    
                    HStack {
                        TextField("e.g. awards-expenditure-attract", text: $tunnelID)
                            .textFieldStyle(PlainTextFieldStyle())
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                        
                        Button(action: { isShowingScanner = true }) {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.title2)
                                .foregroundColor(.blue)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                    )
                }
                .padding(.horizontal)

                if isSearching {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .blue))
                        .scaleEffect(1.5)
                } else if !sessionID.isEmpty {
                    VStack(spacing: 12) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Ready: \(sessionID)")
                        }
                        .font(.headline)
                        .foregroundColor(.green)
                        
                        Button(action: {
                            self.sessionID = ""
                            findActiveSession()
                        }) {
                            Text("Refresh Session")
                                .font(.footnote)
                                .foregroundColor(.blue)
                        }
                    }
                    .padding()
                    .background(Color.green.opacity(0.1))
                    .cornerRadius(10)
                }

                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.footnote)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Spacer()

                Button(action: {
                    if sessionID.isEmpty {
                        findActiveSession()
                    } else {
                        isConnected = true
                    }
                }) {
                    Text(sessionID.isEmpty ? "Search Session" : "Join AR View")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(tunnelID.isEmpty ? Color.gray.opacity(0.5) : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(15)
                        .shadow(color: tunnelID.isEmpty ? .clear : .blue.opacity(0.3), radius: 10, x: 0, y: 5)
                        .padding(.horizontal)
                }
                .disabled(tunnelID.isEmpty || isSearching)
                
                Spacer().frame(height: 20)
            }
            .navigationTitle("")
            .navigationBarHidden(true)
            .sheet(isPresented: $isShowingScanner) {
                ScannerView(scannedCode: $tunnelID)
            }
            .background(
                NavigationLink(
                    destination: FrameViewerView(baseURL: fullBaseURL, sessionID: sessionID),
                    isActive: $isConnected,
                    label: { EmptyView() }
                )
            )
        }
    }

    func findActiveSession() {
        guard !fullBaseURL.isEmpty, let url = URL(string: "\(fullBaseURL)/session/latest") else {
            errorMessage = "Please enter a valid Tunnel ID"
            return
        }

        isSearching = true
        errorMessage = nil

        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                isSearching = false
                if let error = error {
                    errorMessage = "Server unreachable. Check Tunnel ID."
                    return
                }

                if let data = data, let id = String(data: data, encoding: .utf8) {
                    let cleanedID = id.trimmingCharacters(in: .whitespacesAndNewlines)
                    if cleanedID.isEmpty || cleanedID == "NONE" {
                        errorMessage = "Tunnel found, but no active game session."
                    } else {
                        self.sessionID = cleanedID
                    }
                }
            }
        }.resume()
    }
}

// MARK: - Simple QR Scanner View
struct ScannerView: UIViewControllerRepresentable {
    @Binding var scannedCode: String
    @Environment(\.presentationMode) var presentationMode

    func makeUIViewController(context: Context) -> ScannerViewController {
        let viewController = ScannerViewController()
        viewController.delegate = context.coordinator
        return viewController
    }

    func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, ScannerViewControllerDelegate {
        var parent: ScannerView

        init(_ parent: ScannerView) {
            self.parent = parent
        }

        func didFindCode(_ code: String) {
            parent.scannedCode = code
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}

// Simple Camera Logic
protocol ScannerViewControllerDelegate: AnyObject {
    func didFindCode(_ code: String)
}

class ScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    weak var delegate: ScannerViewControllerDelegate?
    var captureSession: AVCaptureSession!
    var previewLayer: AVCaptureVideoPreviewLayer!

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        captureSession = AVCaptureSession()

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }
        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch { return }

        if (captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else { return }

        let metadataOutput = AVCaptureMetadataOutput()

        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else { return }

        previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)

        captureSession.startRunning()
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        if let metadataObject = metadataObjects.first {
            guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
            guard let stringValue = readableObject.stringValue else { return }
            AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
            delegate?.didFindCode(stringValue)
            captureSession.stopRunning()
        }
    }
}
