package cn.edu.hznu.service;

import cn.edu.hznu.util.ArrayUtils;
import cn.edu.hznu.util.PlotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.tablesaw.plotly.Plot;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

@Service
@Slf4j
public class PlotService {

    public void plot(ArrayList<Double> episodes, ArrayList<Double> successRates, String flag) {
        var episodes_ = ArrayUtils.toDoubleArray(episodes);
        var successRates_ = ArrayUtils.toDoubleArray(successRates);
        var figure = PlotUtils.plot(new double[][]{episodes_}, new double[][]{successRates_}, new String[]{"rl"}, "episode", "success rate");
        var path = Paths.get("results", "figure", flag + ".html");
        saveSuccessRates(episodes_, successRates_, "results/log/success_rate_" + flag + ".csv");
        try {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException var2) {
                throw new UncheckedIOException(var2);
            }
            var file = path.toFile();
            Plot.show(figure, file);
        } catch (Exception e) {
            log.info("browser not support!");
        }
    }

    public void saveSuccessRates(double[] x, double[] y, String flag) {
        String filePath = "results/log/success_rate_" + flag + ".csv";
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("episode,success_rate\n"); // header
            for (int i = 0; i < x.length; i++) {
                writer.write(x[i] + "," + y[i] + "\n");
            }
            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save CSV: " + e.getMessage());
        }
    }

    public void saveSuccessRatesStds(double[] x, double[] y, String flag) {
        String filePath = "results/log/success_rate_std_" + flag + ".csv";
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("episode,std\n"); // header
            for (int i = 0; i < x.length; i++) {
                writer.write(x[i] + "," + y[i] + "\n");
            }
            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save CSV: " + e.getMessage());
        }
    }

    public void saveRewards(ArrayList<Double> rewardsList, String flag) {
        var filePath = "results/log/rewards_" + flag + ".csv";
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Rewards\n");
            for (Double reward : rewardsList) {
                writer.append(String.valueOf(reward)).append("\n");
            }
            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save CSV: " + e.getMessage());
        }
    }
}
