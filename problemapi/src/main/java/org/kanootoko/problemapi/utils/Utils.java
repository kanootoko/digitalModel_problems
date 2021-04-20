package org.kanootoko.problemapi.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.kanootoko.problemapi.models.entities.Problem;

public class Utils {
    public static Double[] evaluatePolygon(List<Problem> problems) {
        if (problems.isEmpty()) {
            return new Double[] {null, null, null, null, 0.0};
        }
        Double[] res = new Double[5];
        res[4] = (double) problems.size();
        String filename = java.util.UUID.randomUUID().toString();
        try {
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(new File(filename + ".csv")))) {
                csvWriter.writeNext("ID,Название,Широта,Долгота,Подкатегория,Категория".split(","));
                for (Problem problem: problems) {
                    csvWriter.writeNext(new String[]{problem.getId().toString(), problem.getName(), problem.getLatitude().toString(),
                        problem.getLongitude().toString(), problem.getSubcategory(), problem.getCategory()});
                }
            }
    
            Process p = Runtime.getRuntime().exec(new String[]{"Rscript", "polygon_evaluation.R", filename + ".csv"});
            p.waitFor(15, TimeUnit.SECONDS);
    
            try (CSVReader reader = new CSVReader(new FileReader(new File(filename + "_output.csv")))) {
                reader.readNext();
                String[] tmp = reader.readNext();
                res[0] = Double.parseDouble(tmp[0]);
                res[1] = Double.parseDouble(tmp[1]);
                res[2] = Double.parseDouble(tmp[2]);
                res[3] = Double.parseDouble(tmp[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            new File(filename + ".csv").delete();
            new File(filename + "_output.csv").delete();
        }
        return res;
    }
    public static List<Double[]> evaluateObjects(List<Problem> problems, String objectType) {
        String filename = java.util.UUID.randomUUID().toString();
        List<Double[]> res = new ArrayList<>();
        try {
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(new File(filename + ".csv")))) {
                csvWriter.writeNext("ID,Название,Широта,Долгота,Подкатегория,Категория".split(","));
                for (Problem problem: problems) {
                    csvWriter.writeNext(new String[]{problem.getId().toString(), problem.getName(), problem.getLatitude().toString(),
                        problem.getLongitude().toString(), problem.getSubcategory(), problem.getCategory()});
                }
            }
    
            Process p = Runtime.getRuntime().exec(new String[]{"Rscript", "every_object_evaluation.R", filename + ".csv", objectType});
            p.waitFor(15, TimeUnit.SECONDS);
            
            try (CSVReader reader = new CSVReader(new FileReader(new File(filename + "_output.csv")))) {
                String[] tmp;
                reader.readNext();
                while ((tmp = reader.readNext()) != null) {
                    Double[] coordsAndSICT = new Double[6];
                    coordsAndSICT[0] = Double.parseDouble(tmp[0]);
                    coordsAndSICT[1] = Double.parseDouble(tmp[1]);
                    coordsAndSICT[2] = Double.parseDouble(tmp[2]);
                    coordsAndSICT[3] = Double.parseDouble(tmp[3]);
                    coordsAndSICT[4] = Double.parseDouble(tmp[4]);
                    coordsAndSICT[5] = Double.parseDouble(tmp[5]);
                    res.add(coordsAndSICT);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            new File(filename + ".csv").delete();
            new File(filename + "_output.csv").delete();
        }
        return res;
    }
}