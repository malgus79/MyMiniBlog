package com.myminiblog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.myminiblog.SnapshotsApplication.Companion.PATH_SNAPSHOTS
import com.myminiblog.databinding.FragmentAddBinding

class AddFragment : Fragment() {


    private val RC_GALLLERY = 1
    private lateinit var mBinding: FragmentAddBinding
    private var mPhotoSelectedUri: Uri? = null
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDatabaseReference: DatabaseReference

    private val galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                mPhotoSelectedUri = it.data?.data

                mBinding.imgPhoto.setImageURI(mPhotoSelectedUri)
                mBinding.tilTitle.visibility = View.VISIBLE
                mBinding.tvMessage.text = getString(R.string.post_message_valid_title)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mBinding = FragmentAddBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //configurar los botones
        mBinding.btnPost.setOnClickListener { postSnapshot() }
        mBinding.btnSelect.setOnClickListener { openGallery() }

        //inicializar var de referencia
        mStorageReference = FirebaseStorage.getInstance().reference
        mDatabaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOTS)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryResult.launch(intent)
    }

    //subur imagen a storage
    private fun postSnapshot() {
        mBinding.progressBar.visibility = View.VISIBLE
        val key = mDatabaseReference.push().key!!

        //ruta donde se guardara en storaje
        val storageReference = mStorageReference.child(PATH_SNAPSHOTS)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(key)

        if (mPhotoSelectedUri != null) {
            storageReference.putFile(mPhotoSelectedUri!!)
                //pintar el progres conforme se vaya subiendo la imagen
                .addOnProgressListener {
                    val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                    mBinding.progressBar.progress = progress.toInt()
                    mBinding.tvMessage.text = "$progress"
                }
                .addOnCompleteListener {
                    mBinding.progressBar.visibility = View.INVISIBLE
                }
                .addOnSuccessListener {
                    Snackbar.make(mBinding.root, "Instantanea publicada", Snackbar.LENGTH_SHORT)
                        .show()
                    it.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveSnapshot(key,
                            downloadUri.toString(),
                            mBinding.etTitle.text.toString().trim())

                        mBinding.tilTitle.visibility = View.GONE
                        mBinding.tvMessage.text = getString(R.string.post_message_title)
                    }
                }
                .addOnFailureListener {
                    Snackbar.make(mBinding.root,
                        "No se pudo subir, intente mas tarde",
                        Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    //guardar la url dentro de database
    private fun saveSnapshot(key: String, url: String, title: String) {
        val snapshot = Snapshot(title = title, photoUrl = url)
        mDatabaseReference.child(key).setValue(snapshot)
//            .addOnSuccessListener {
//                hideKeyboard()
//                mainAux?.showMessage(R.string.post_message_post_success)
//
//                with(mBinding) {
//                    tilTitle.visibility = View.GONE
//                    etTitle.setText("")
//                    tilTitle.error = null
//                    tvMessage.text = getString(R.string.post_message_title)
//                    imgPhoto.setImageDrawable(null)
//                }
//            }
//            .addOnCompleteListener { enableUI(true) }
//            .addOnFailureListener { mainAux?.showMessage(R.string.post_message_post_snapshot_fail) }
    }


}