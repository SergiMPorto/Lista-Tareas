package com.example.todolist;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private String tareaAEditar;


    ListView listViewTareas;
    ArrayAdapter<String> adapterTareas;

    List<String> listaTareas = new ArrayList<>();
    List<String> listaIdeTareas = new ArrayList<>();

    EditText editTextTarea;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        adapterTareas = new ArrayAdapter<>(this, R.layout.tareas, R.id.textViewTarea, listaTareas);
        listViewTareas.setAdapter(adapterTareas);

        editTextTarea = findViewById(R.id.editTextTarea);

        listViewTareas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tareaSeleccionada = listaTareas.get(position);
                mostrarDialogoEdicion(tareaSeleccionada);
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
                .setMessage("Introduce tu nueva tarea")
                .setView(tarea)
                .setPositiveButton("Añadir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nuevaTarea = tarea.getText().toString();
                        Toast.makeText(MainActivity.this, "Tarea añadida: " + nuevaTarea, Toast.LENGTH_SHORT).show();

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

    private void mostrarDialogoEdicion(final String tareaSeleccionada) {
        tareaAEditar = tareaSeleccionada;

        final EditText editText = new EditText(this);
        editText.setText(tareaSeleccionada);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Tarea");
        builder.setMessage("Ingrese el nuevo título:");
        builder.setView(editText);

        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nuevoTexto = editText.getText().toString();
                actualizarTareaEnBD(tareaSeleccionada, nuevoTexto);
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


    private void actualizarTareaEnBD(String tareaOriginal, String nuevaTarea) {
        int posicion = listaTareas.indexOf(tareaOriginal);
        if (posicion != -1) {
            String idTarea = listaIdeTareas.get(posicion);

            Map<String, Object> data = new HashMap<>();
            data.put("nombreTarea", nuevaTarea);

            db.collection("Tareas").document(idTarea)
                    .update(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MainActivity.this, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                            actualizarUI();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Error al actualizar la tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e("MainActivity", "Error: No se encontró la tarea en la lista");
        }
    }

    private void actualizarUI() {
        db.collection("Tareas")
                .whereEqualTo("usuario", idUser)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("MainActivity", "Error al escuchar cambios en la base de datos", e);
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
        View parent = (View) view.getParent();
        TextView tareaTextView = parent.findViewById(R.id.textViewTarea);
        String tarea = tareaTextView.getText().toString();
        mostrarDialogoEdicion(tarea);
    }

    public void guardarEdicion(View view) {
        String nuevaTarea = editTextTarea.getText().toString();
        if (!nuevaTarea.isEmpty()) {
            actualizarTareaEnBD(tareaAEditar, nuevaTarea);
        } else {
            Toast.makeText(this, "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show();
        }
        resetUI();
    }

    public void cancelarEdicion(View view) {
        resetUI();
    }

    private void resetUI() {
        editTextTarea.setVisibility(View.GONE);
        editTextTarea.setText("");

        findViewById(R.id.btnGuardar).setVisibility(View.GONE);
        findViewById(R.id.btnCancelar).setVisibility(View.GONE);

        findViewById(R.id.button).setVisibility(View.VISIBLE);
        findViewById(R.id.buttoneditar).setVisibility(View.VISIBLE);
    }
}
