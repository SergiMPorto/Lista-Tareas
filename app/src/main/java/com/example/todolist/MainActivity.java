package com.example.todolist;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String idUser;

    ListView listViewTareas;
    ArrayAdapter<String> adapterTareas;

    List<String> listaTareas = new ArrayList<>();
    List<String> listaIdeTareas = new ArrayList<>();

    EditText editTextTarea;
    TextView textViewTarea;
    Button btnGuardar;
    Button btnCancelar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            idUser = mAuth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listViewTareas = findViewById(R.id.tarea);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapterTareas = new ArrayAdapter<>(this, R.layout.tareas, R.id.textViewTarea, listaTareas);
        listViewTareas.setAdapter(adapterTareas);

        editTextTarea = findViewById(R.id.editTextTarea);
        textViewTarea = findViewById(R.id.textViewTarea);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        listViewTareas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tareaSeleccionada = listaTareas.get(position);

            }
        });

        actualizarUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add) {

            mostrarDialogoNuevaTarea();
            return true;
        } else if (item.getItemId() == R.id.out) {
            // Cerrar sesión
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void mostrarDialogoNuevaTarea() {
        final EditText tarea = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo Título")
                .setMessage("Introduce tu nueva lectura")
                .setView(tarea)
                .setPositiveButton("Añadir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nuevaTarea = tarea.getText().toString();
                        Toast.makeText(MainActivity.this, "Libro añadido: " + nuevaTarea, Toast.LENGTH_SHORT).show();

                        Map<String, Object> data = new HashMap<>();
                        data.put("nombreTarea", nuevaTarea);
                        data.put("usuario", idUser);

                        db.collection("Tareas")
                                .add(data)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(MainActivity.this, "Tarea añadida: " + nuevaTarea, Toast.LENGTH_SHORT).show();
                                        actualizarUI();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Fallo al crear la tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();
    }

    private void actualizarTareaEnBD(int posicion, String nuevaTarea) {
        String idTarea = listaIdeTareas.get(posicion);

        Map<String, Object> data = new HashMap<>();
        data.put("nombreTarea", nuevaTarea);

        db.collection("Tareas").document(idTarea)
                .update(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Libro actualizado", Toast.LENGTH_SHORT).show();
                        actualizarUI();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Error al actualizar el libro", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarUI() {
        db.collection("Tareas")
                .whereEqualTo("usuario", idUser)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        listaTareas.clear();
                        listaIdeTareas.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            listaTareas.add(doc.getString("nombreTarea"));
                            listaIdeTareas.add(doc.getId());
                        }

                        adapterTareas.notifyDataSetChanged();
                    }
                });
    }

    public void borrarTarea(View view) {
        View parent = (View) view.getParent();
        TextView tareaTextView = parent.findViewById(R.id.textViewTarea);
        String tarea = tareaTextView.getText().toString();
        int posicion = listaTareas.indexOf(tarea);
        db.collection("Tareas").document(listaIdeTareas.get(posicion)).delete();
        actualizarUI();
    }

    public void editarTarea(View view) {
        final EditText editText = new EditText(this);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Libro");
        builder.setMessage("Ingrese el título:");


        builder.setView(editText);


        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nuevoTexto = editText.getText().toString();
                actualizarTareaEnBD(listaTareas.indexOf(textViewTarea.getText().toString()), nuevoTexto);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }
}
