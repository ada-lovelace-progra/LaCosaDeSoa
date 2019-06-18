package com.example.appsistentedecocina;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends NGActivity {

    private Button btnIng;
    private Button btnRec;
    private Button btnBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnIng = (Button) findViewById(R.id.bt_ingrediente);
        btnIng.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view){
                Intent intent;
                intent=new Intent(MainActivity.this, List_Ingrediente.class);
                startActivity(intent);
            }
        });

        btnRec = (Button) findViewById(R.id.bt_recetas);
        btnRec.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view){
                Intent intent;
                intent=new Intent(MainActivity.this, Menu_Recetas.class);
                startActivity(intent);
            }
        });

        btnBt = (Button) findViewById(R.id.bt_bluetooth);
        btnBt.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view){
                Intent intent;
                intent=new Intent(MainActivity.this, BluetoothBusqueda.class);
                startActivity(intent);
            }
        });

        iniciarServicioBluetooth();
        vincularServicioBluetooth();
    }
}
