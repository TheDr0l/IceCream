package com.example.IceCream_SpringBoot.service;

import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;

import java.io.InputStream;
import java.util.ArrayList;

@Service
public class WekaPrecioService {

    private final Classifier modelo;
    private final Instances estructura;

    public WekaPrecioService() throws Exception {
        // 1. Cargar el modelo M5P
        InputStream modelStream = getClass().getResourceAsStream("/wekaModels/PredecirPrecio.model");
        modelo = (Classifier) weka.core.SerializationHelper.read(modelStream);

        // 2. Definir atributos en el orden correcto que el arvhivo ARFF de entrenamiento
        ArrayList<Attribute> atributos = new ArrayList<>();

        // --- indice 0: Sabor (Nominal) ---
        ArrayList<String> sabores = new ArrayList<>();
        sabores.add("mango");
        sabores.add("vainilla");
        sabores.add("coco");
        sabores.add("fresa");
        sabores.add("maracuya");
        sabores.add("mora");
        sabores.add("menta");
        sabores.add("cafe");
        sabores.add("chocolate");
        sabores.add("caramelo");
        sabores.add("nutella");
        sabores.add("limon");
        sabores.add("durazno");
        atributos.add(new Attribute("sabor", sabores));

        // --- indice 1: Tipo (Nominal) ---
        ArrayList<String> tipos = new ArrayList<>();
        tipos.add("paleta");
        tipos.add("vaso");
        tipos.add("cono");
        tipos.add("galleta");
        atributos.add(new Attribute("tipo", tipos));

        // --- indice 2: Precio Unitario (Numerico - CLASE A PREDECIR) ---
        atributos.add(new Attribute("precioUnitario"));

        // Crear estructura
        estructura = new Instances("PrediccionPrecio", atributos, 0);
        
        // IMPORTANTE: Establecer que la clase a predecir es el atributo 2 (Precio)
        estructura.setClassIndex(2); 
    }

    public double predecirPrecio(String sabor, String tipo) throws Exception {
        // Instancia con 3 atributos
        Instance instancia = new DenseInstance(3);
        instancia.setDataset(estructura);

        // Asignamos los valores conocidos
        instancia.setValue(0, sabor);
        instancia.setValue(1, tipo);
        
        double precioPredicho = modelo.classifyInstance(instancia);
        
        return precioPredicho;
    }
}