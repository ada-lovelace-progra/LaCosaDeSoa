package com.example.appsistentedecocina;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static android.app.PendingIntent.getActivity;

public class List_Ingrediente extends NGActivity {

    private ListView listview;
    private AdaptadorIngrediente adapter;
    private ArrayList<Ingrediente> listaIngredientes = new ArrayList<Ingrediente>();
    private String nuevo_ingrediente = "";
    private int nueva_cantidad = 0;
    private int nuevo_id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list__ingrediente);
        setTitle("Lista de Ingredientes");

        final Context context = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Nuevo Ingrediente");

                //contexto
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

                // Set up the input
                final TextView ingrediente = new TextView(context);
                final EditText input = new EditText(context);
                final TextView cantidad = new TextView(context);
                final EditText input2 = new EditText(context);
                final TextView id = new TextView(context);
                final EditText input3 = new EditText(context);

                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                ingrediente.setText(" ingrediente:");
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
                cantidad.setText(" cantidad (gr):");
                input2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_NUMBER);
                id.setText("id: ");
                input3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_NUMBER);

                //lo meto en el layout este
                layout.addView(ingrediente);
                layout.addView(input);
                layout.addView(cantidad);
                layout.addView(input2);
                layout.addView(id);
                layout.addView(input3);

                //lo seteo
                builder.setView(layout);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!input.getText().toString().equals("")) {
                            nuevo_ingrediente = input.getText().toString();
                            nueva_cantidad = Integer.parseInt(input2.getText().toString());
                            nuevo_id = Integer.parseInt(input3.getText().toString());
                            add_ingrediente();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

            }
        });

        listview = (ListView) findViewById(R.id.ingrediente_list);

        if (!list_exists(this))
        {
            create_list();
        }
        open_list();

        adapter = new AdaptadorIngrediente(this, listaIngredientes);
        listview.setAdapter(adapter);
    }

    private void add_ingrediente() {

        if (nuevo_ingrediente.equals("") || nueva_cantidad <= 0) {
            Toast.makeText(getApplicationContext(), "pusiste valores invalidos", Toast.LENGTH_SHORT).show();
            return;
        }
        listaIngredientes.add(new Ingrediente(nuevo_ingrediente, nueva_cantidad, (byte)nuevo_id));
        adapter = new AdaptadorIngrediente(this, listaIngredientes);
        listview.setAdapter(adapter);

        nuevo_ingrediente = "";
        nueva_cantidad = 0;
    }

    /**
     * es necesario refrescar la lista on resume (para cuando cambia el layout y toda la joda)
     */
    @Override
    protected void onResume() {
        super.onResume();
        adapter = new AdaptadorIngrediente(this, listaIngredientes);
        listview.setAdapter(adapter);
    }


    private void create_list() {
        File file = new File(this.getFilesDir(), "ingredientes");
        file.setWritable(true);
        file.setReadable(true);
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(this.listaIngredientes);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void save_list() {
        File directory = this.getFilesDir();
        File file = new File(directory, "ingredientes");
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(this.listaIngredientes);
        } catch (Exception e) {
            Log.d("List_Ingrediente", "error al abrir archivo: " + e.getMessage());
        } finally {
            try {
                if (oos != null) oos.close();
                if (fos != null) fos.close();
            } catch (Exception e) { /* do nothing */ }
        }

    }

    private void open_list() {
        File directory = this.getFilesDir();
        File file = new File(directory, "ingredientes");
        FileInputStream fis = null;
        ObjectInput ois = null;

        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            this.listaIngredientes = (ArrayList<Ingrediente>) ois.readObject();
        } catch (Exception e) {
            Log.d("List_Ingrediente", "error al abrir archivo: " + e.getMessage());
        } finally {
            try {
                if (ois != null) ois.close();
                if (fis != null) fis.close();
            } catch (Exception e) { /* do nothing */ }
        }
    }

    /**
     * devuelve true si existe un archivo con ingredientes
     * @param context contexto
     * @return true si existe, false si no existe
     */
    public boolean list_exists(Context context) {
        File file = context.getFileStreamPath("ingredientes");
        if (file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        save_list();
        super.onDestroy();
    }
}
