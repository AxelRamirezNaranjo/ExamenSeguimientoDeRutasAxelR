package com.example.seguimientoderutas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var databaseReference: DatabaseReference? = null
    private var locationCallback: LocationCallback? = null
    private val puntosRutaEnTiempoReal = mutableListOf<LatLng>()

    private var mMap: GoogleMap? = null
    private lateinit var btnIniciar: Button
    private lateinit var btnDetener: Button
    private lateinit var btnHistorial: Button
    private lateinit var tvDistancia: TextView
    private lateinit var tvDuracion: TextView

    private var startTime: Long = 0
    private var totalDistance: Float = 0f

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    data class Ruta(val latitude: Double, val longitude: Double, val timestamp: Long, val duration: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        tvDistancia = findViewById(R.id.tvDistancia)
        tvDuracion = findViewById(R.id.tvDuracion)

        // Inicializar Firebase y el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        databaseReference = FirebaseDatabase.getInstance().getReference("rutas")

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar los botones
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        btnHistorial = findViewById(R.id.btnHistorial)

        // Configurar los botones
        btnIniciar.setOnClickListener { iniciarGrabacion() }
        btnDetener.setOnClickListener { detenerGrabacion() }

        // Manejar clic en el botón Historial
        btnHistorial.setOnClickListener {
            val intent = Intent(this, HistorialRutas::class.java)
            startActivity(intent)
        }

        // Deshabilitar el botón de detener por defecto
        btnDetener.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar ubicación si los permisos están concedidos
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap!!.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Mostrar la ruta guardada en el mapa
        mostrarRuta()
    }

    private fun iniciarGrabacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult == null) return
                    for (location in locationResult.locations) {
                        guardarUbicacion(location) // Guardar y actualizar mapa
                    }
                }
            }

            fusedLocationClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Registrar tiempo de inicio
            startTime = System.currentTimeMillis()

            // Cambiar el estado de los botones
            btnIniciar.isEnabled = false // Deshabilitar el botón de iniciar
            btnDetener.isEnabled = true // Habilitar el botón de detener
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun guardarUbicacion(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        puntosRutaEnTiempoReal.add(latLng)

        // Dibuja la ruta en tiempo real
        dibujarRutaEnMapa(puntosRutaEnTiempoReal)

        // Calcular distancia total usando Location
        if (puntosRutaEnTiempoReal.size > 1) {
            val prevLocation = Location("")
            prevLocation.latitude = puntosRutaEnTiempoReal[puntosRutaEnTiempoReal.size - 2].latitude
            prevLocation.longitude = puntosRutaEnTiempoReal[puntosRutaEnTiempoReal.size - 2].longitude

            val currentLocation = Location("")
            currentLocation.latitude = location.latitude
            currentLocation.longitude = location.longitude

            // Calcular la distancia entre las dos ubicaciones
            totalDistance += prevLocation.distanceTo(currentLocation)
        }

        // Actualizar la distancia en pantalla
        tvDistancia.text = "Distancia total: ${totalDistance.toInt()} m"

        // Calcular y mostrar la duración
        val duration = (System.currentTimeMillis() - startTime) / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        tvDuracion.text = String.format("Duración total: %02d:%02d", minutes, seconds)

        // Crear un objeto Ruta y guardar en Firebase
        val rutaId = databaseReference!!.push().key
        if (rutaId != null) {
            val nuevaRuta = Ruta(location.latitude, location.longitude, System.currentTimeMillis(), duration)
            databaseReference!!.child(rutaId).setValue(nuevaRuta)
                .addOnSuccessListener {
                    Toast.makeText(this, "Ruta guardada exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar la ruta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun detenerGrabacion() {
        // Detener las actualizaciones de ubicación
        if (locationCallback != null) {
            fusedLocationClient!!.removeLocationUpdates(locationCallback!!)

            // Opcionalmente, limpiar la lista de puntos de la ruta
            puntosRutaEnTiempoReal.clear()

            // Limpiar la ruta del mapa
            mMap?.clear()

            // Cambiar el estado de los botones
            btnIniciar.isEnabled = true // Habilitar el botón de iniciar
            btnDetener.isEnabled = false // Deshabilitar el botón de detener

            Toast.makeText(this, "Grabación detenida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarRuta() {
        databaseReference!!.get().addOnSuccessListener { dataSnapshot ->
            for (snapshot in dataSnapshot.children) {
                val ruta = snapshot.getValue(Ruta::class.java)
                if (ruta != null) {
                    val latLng = LatLng(ruta.latitude, ruta.longitude)
                    puntosRutaEnTiempoReal.add(latLng)
                }
            }
            dibujarRutaEnMapa(puntosRutaEnTiempoReal)
        }
    }

    private fun dibujarRutaEnMapa(puntos: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(puntos)
            width(10f)
            color(android.graphics.Color.BLUE)
        }
        mMap?.addPolyline(polylineOptions)

        // Ajustar la cámara para mostrar toda la ruta
        if (puntos.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            for (latLng in puntos) {
                boundsBuilder.include(latLng)
            }
            val bounds = boundsBuilder.build()
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
            mMap?.animateCamera(cameraUpdate)
        }
    }
}

