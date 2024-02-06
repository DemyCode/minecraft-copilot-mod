package net.demycode.minecraft_copilot_mod;

import java.io.File;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class ModelDownloader extends Thread {
    public String modelId = null;
    public boolean isDownloaded = false;

    public ModelDownloader(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public void run() {
        this.isDownloaded = false;
        S3Client s3 = S3Client.builder().region(Region.EU_WEST_3).build();
        File localFile = new File("model.onnx");
        GetObjectRequest getObjectRequest = GetObjectRequest
                .builder()
                .bucket("minecraft-copilot-models")
                .key(this.modelId)
                .build();
        try {
            s3.getObject(getObjectRequest, localFile.toPath());
            this.isDownloaded = true;
        } catch (Exception e) {
            System.out.println("Failed to download model");
            e.printStackTrace();
        }
    }
}
