package com.myminiblog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.core.SnapshotHolder
import com.google.firebase.storage.FirebaseStorage
import com.myminiblog.databinding.FragmentHomeBinding
import com.myminiblog.databinding.ItemSnapshotBinding

class HomeFragment : Fragment() {

    private lateinit var mBinding: FragmentHomeBinding

    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Snapshot, SnapshotHolder>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = FirebaseDatabase.getInstance().reference.child("snapshots")

        //identificador de cada imagen dependiendo de como se llama la rama
        val options =
            FirebaseRecyclerOptions.Builder<Snapshot>().setQuery(query, SnapshotParser {
                val snapshot = it.getValue(Snapshot::class.java)
                snapshot!!.id = it.key!!
                snapshot
            }).build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Snapshot, SnapshotHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_snapshot, parent, false)
                return SnapshotHolder(view)
            }

            override fun onBindViewHolder(holder: SnapshotHolder, position: Int, model: Snapshot) {
                val snapshot = getItem(position)

                with(holder) {
                    setListener(snapshot)

                    with(binding) {
                        tvTitle.text = snapshot.title
                        cbLike.text =
                            snapshot.likeList.keys.size.toString() //cuantos likes tiene la imagen
                        FirebaseAuth.getInstance().currentUser?.let {
                            cbLike.isChecked = snapshot.likeList  //si el usuario le dio like o no
                                .containsKey(it.uid)
                        }

                        Glide.with(mContext)
                            .load(snapshot.photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(imgPhoto)
                    }
                }
            }

            //para acultar el progressbar
            @SuppressLint("NotifyDataSetChanged")//error interno firebase ui 8.0.0
            override fun onDataChanged() {
                super.onDataChanged()
                mBinding.progressBar.visibility = View.GONE
                notifyDataSetChanged()
            }

            //mostrar error
            override fun onError(error: DatabaseError) {
                super.onError(error)
                Toast.makeText(mContext, error.message, Toast.LENGTH_SHORT).show()
                //Snackbar.make(mBinding.root, error.message, Snackbar.LENGTH_SHORT).show()
            }
        }

        //configuracion del recyclerView
        mLayoutManager = LinearLayoutManager(context)

        mBinding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }
    }

    //cuando se van a comenzar a consumir los datos
    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        mFirebaseAdapter.stopListening()
    }

    //eliminar imagen
    private fun deleteSnapshot(snapshot: Snapshot) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots")
        databaseReference.child(snapshot.id).removeValue()
    }

    //evento de likes
    private fun setLike(snapshot: Snapshot, checked: Boolean) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots")
        if (checked) {
            databaseReference.child(snapshot.id).child("likeList")
                .child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(checked)
        } else {
            databaseReference.child(snapshot.id).child("likeList")
                .child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(null)
        }
    }


    inner class SnapshotHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemSnapshotBinding.bind(view)

        //eventos de eliminar y like de cada imagen
        fun setListener(snapshot: Snapshot) {
            binding.btnDelete.setOnClickListener { deleteSnapshot(snapshot) }
            binding.cbLike.setOnCheckedChangeListener { _, checked ->
                setLike(snapshot, checked)
            }

        }
    }
}