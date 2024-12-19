package com.example.seguimientoderutas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.seguimientoderutas.Ruta;
import com.example.seguimientoderutas.Coordenada;

import java.util.ArrayList;
import java.util.List;

public class HistorialRutas extends AppCompatActivity {

    private RecyclerView rvRutas;
    private RutaAdapter rutaAdapter;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_rutas);

        // Inicializamos el RecyclerView y Firebase
        rvRutas = findViewById(R.id.rvRutas);
        databaseReference = FirebaseDatabase.getInstance().getReference("rutas");

        // Configuramos el RecyclerView
        rvRutas.setLayoutManager(new LinearLayoutManager(this));

        // Cargamos las rutas desde Firebase
        mostrarRutas();

    }

    private void mostrarRutas() {
        databaseReference.get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                List<String> rutas = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String rutaId = snapshot.getKey();
                    if (rutaId != null) {
                        rutas.add(rutaId);
                    }
                }

                // Configuramos el adapter con las rutas obtenidas
                rutaAdapter = new RutaAdapter(rutas, new RutaAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(String rutaId) {
                        // Acción cuando se selecciona una ruta
                        verRutaEnMapa(rutaId);
                    }
                });

                // Establecemos el adapter al RecyclerView
                rvRutas.setAdapter(rutaAdapter);
            } else {
                Toast.makeText(this, "No se han encontrado rutas", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar las rutas", Toast.LENGTH_SHORT).show();
        });
    }

    private void verRutaEnMapa(String rutaId) {
        // Aquí puedes implementar la lógica para ver la ruta seleccionada en el mapa
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("rutaId", rutaId);
        startActivity(intent);
    }
}
