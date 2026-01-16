//
//  FrameViewerView.swift
//  iAetherShell
//
//  Created by Bhaskar Das on 2026-01-15.
//


import SwiftUI

struct FrameViewerView: View {
    let baseURL: String
    let sessionID: String
    
    // We keep these to handle the data loading
    @State private var frames: [UIImage] = []
    @State private var isLoading = true

    var body: some View {
        ZStack {
            if isLoading {
                // While downloading, show a simple loading state
                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text("Fetching NDS Sketches...")
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
            } else {
                // THE AR WORLD
                // This replaces the old 2D Image logic
                ARContainer(frames: $frames)
                    .edgesIgnoringSafeArea(.all)
                
                // UI Overlay
                VStack {
                    HStack {
                        Text("AR Active: \(sessionID)")
                            .font(.caption)
                            .padding(8)
                            .background(.ultraThinMaterial)
                            .cornerRadius(8)
                        Spacer()
                    }
                    .padding()
                    
                    Spacer()
                    
                    Text("Point at a table or wait for fallback...")
                        .font(.footnote)
                        .padding()
                        .background(.black.opacity(0.5))
                        .foregroundColor(.white)
                        .cornerRadius(20)
                        .padding(.bottom, 30)
                }
            }
        }
        .onAppear {
            loadFrames()
        }
        .navigationTitle("AetherShell AR")
        .navigationBarTitleDisplayMode(.inline)
    }

    func loadFrames() {
        let maxFrames = 50
        let group = DispatchGroup()
        var tempFrames: [UIImage] = []

        for i in 0..<maxFrames {
            let urlStr = "\(baseURL)/output/\(sessionID)_frame_\(i).png"
            guard let url = URL(string: urlStr) else { continue }

            group.enter()
            URLSession.shared.dataTask(with: url) { data, _, _ in
                if let data = data, let img = UIImage(data: data) {
                    tempFrames.append(img)
                }
                group.leave()
            }.resume()
        }

        // Once all images are downloaded, update the UI
        group.notify(queue: .main) {
            if !tempFrames.isEmpty {
                self.frames = tempFrames
                self.isLoading = false
            }
        }
    }
}
