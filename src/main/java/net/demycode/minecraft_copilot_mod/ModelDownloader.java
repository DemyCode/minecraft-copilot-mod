package net.demycode.minecraft_copilot_mod;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URL;

public class ModelDownloader extends Thread {
    public String modelPath = null;
    public String jsonIdToInt = null;
    public String localModelPath = null;
    public String localJsonIdToIntPath = null;
    public boolean isDownloaded = false;

    public ModelDownloader(String remoteModelPath, String remoteJsonIdToIntPath,
            String localModelPath, String localJsonIdToIntPath) {
        this.modelPath = remoteModelPath;
        this.jsonIdToInt = remoteJsonIdToIntPath;
        this.localModelPath = localModelPath;
        this.localJsonIdToIntPath = localJsonIdToIntPath;
    }

    public void download(String remoteFilePath, String localFilePath) {
        try {
            URL url = new URL(remoteFilePath);
            BufferedInputStream in = new BufferedInputStream(url.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(localFilePath);
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.download(this.jsonIdToInt, "id_to_int.json");
        this.download(this.modelPath, "model.onnx");
        this.isDownloaded = true;
    }
}
