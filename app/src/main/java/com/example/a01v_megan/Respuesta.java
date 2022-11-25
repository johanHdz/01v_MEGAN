package com.example.a01v_megan;

//Clase Respuesta
public class Respuesta {
    // Atributos
    private String cuestion;
    private String respuestas;

    //Constructor
    public Respuesta(String cuestion, String respuestas){
        this.cuestion = cuestion;
        this.respuestas = respuestas;
    }

    //Métodos GETs
    public String getCuestion(){
        return  cuestion;
    }

    public String getRespuestas(){
        return respuestas;
    }
}
