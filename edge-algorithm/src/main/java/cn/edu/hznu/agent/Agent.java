package cn.edu.hznu.agent;

import ai.djl.Model;

public interface Agent {

    int selectAction(float[] state, int[] availAction, boolean training);

    void train(boolean isOffline);

    void saveModel(String flag, int i);

    void loadModel(String flag, int i);

    Model getActorModel();

//    void saveHdfsModel(String flag);

//    void loadHdfsModel(String flag);

//    void loadSteamModel(InputStream inputStream, String fileName);
}
