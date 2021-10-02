package com.example.cakeshop.Adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cakeshop.Modal.Cart;
import com.example.cakeshop.R;
import com.example.cakeshop.eventBus.MyUpdateCartEvent;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyCartViewHolder> {

    private Context context;
    private List<Cart> cartList;


    public CartAdapter(Context context, List<Cart> cartList, Activity activity) {
        this.context = context;
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public MyCartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyCartViewHolder((LayoutInflater.from(context).inflate(R.layout.cart_item_list,parent,false)));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCartViewHolder holder, int position) {
        Glide.with(context).load(cartList.get(position).getImage()).into(holder.imageView);
        holder.txtPrice.setText(new StringBuffer("Price : RS. ").append(cartList.get(position).getPrice()));
        holder.txtName.setText(new StringBuffer().append(cartList.get(position).getName()));
        holder.txtQty.setText(new StringBuffer().append(cartList.get(position).getQty()));

        holder.btnMinus.setOnClickListener(v ->{
            minusCartItems(holder,cartList.get(position));
        });
        holder.btnPluse.setOnClickListener(v ->{
            plusCartItems(holder,cartList.get(position));
        });
        //delete
        holder.btnDelete.setOnClickListener(v ->{
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Delete cart item")
                    .setMessage("Do you really want to delete " + cartList.get(position).getName().toString() + "from cart")
                    .setNegativeButton("CANCEL", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("YES", (dialog12, which) -> {
                        notifyItemRemoved(position);
                        deleteFromFirebase(cartList.get(position));
                        dialog12.dismiss();
                    }).create();
            dialog.show();
        });



    }
    //delete from firebase cart item
    private void deleteFromFirebase(Cart cart) {

        FirebaseDatabase.getInstance().getReference("Cart").child("001").child(cart.getKey())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    EventBus.getDefault().postSticky(new MyUpdateCartEvent());
                });
    }

    private void plusCartItems(MyCartViewHolder holder, Cart cart) {
        cart.setQty(cart.getQty() + 1);
        cart.setToatalPrice(cart.getQty() * Float.parseFloat(cart.getPrice()));

        holder.txtQty.setText(new StringBuilder().append(cart.getQty()));
        updateFirebase(cart);
    }

    private void minusCartItems(MyCartViewHolder holder, Cart cart) {
        if(cart.getQty() > 1) {
            cart.setQty(cart.getQty() - 1);
            cart.setToatalPrice(cart.getQty() * Float.parseFloat(cart.getPrice()));

            holder.txtQty.setText(new StringBuilder().append(cart.getQty()));
            updateFirebase(cart);
        }
    }

    private void updateFirebase(Cart cart) {
        FirebaseDatabase.getInstance().getReference("Cart").child("001").child(cart.getKey())
                .setValue(cart)
                .addOnSuccessListener(aVoid -> EventBus.getDefault().postSticky(new MyUpdateCartEvent()));
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public class MyCartViewHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.btnReduse)
        ImageView btnMinus;
        @BindView(R.id.btnAdd)
        ImageView btnPluse;
        @BindView(R.id.btnDelete)
        ImageView btnDelete;
        @BindView(R.id.imgPic)
        ImageView imageView;
        @BindView(R.id.lblPrice)
        TextView txtPrice;
        @BindView(R.id.lblQty)
        TextView txtQty;
        @BindView(R.id.lblName)
        TextView txtName;

        Unbinder unbinder;
        public MyCartViewHolder(@NonNull View itemView) {
            super(itemView);

            unbinder = ButterKnife.bind(this,itemView);

        }
    }
}
