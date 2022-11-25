package com.example.a01v_megan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class Carga extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.carga_layout);

        //Agregar las animaciones
        Animation animacion1 = AnimationUtils.loadAnimation(this, R.anim.desplazamiento_arriba);
        Animation animacion2 = AnimationUtils.loadAnimation(this, R.anim.desplazamiento_abajo);

        TextView textSoftgem = findViewById(R.id.textSoftgem);
        TextView textFrom = findViewById(R.id.textFrom);
        ImageView imagenLogo =  findViewById(R.id.imagenLogo);

        textSoftgem.setAnimation(animacion2);
        textFrom.setAnimation(animacion2);
        imagenLogo.setAnimation(animacion1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Carga.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 4000);

    }
}