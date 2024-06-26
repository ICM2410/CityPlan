import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.primeraentrega.ImageDetailActivity
import com.example.primeraentrega.R

class PhotoGalleryAdapter(private val photoList: List<Uri>) :
    RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_gallery, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUri = photoList[position]
        Glide.with(holder.itemView.context)
            .load(photoUri)
            .placeholder(R.drawable.camara)
            .override(1000, 1000) // Ajustar el tamaño de la imagen aquí (ancho, alto)
            .into(holder.imageViewPhoto)


        // Manejar clics en las imágenes
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ImageDetailActivity::class.java)
            intent.putExtra("imageUri", photoUri.toString())
            holder.itemView.context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int = photoList.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPhoto: ImageView = itemView.findViewById(R.id.imageViewPhoto)
    }

}
