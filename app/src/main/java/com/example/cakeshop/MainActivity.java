package com.example.cakeshop;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cakeshop.Listner.ICartListner;
import com.example.cakeshop.Modal.Cart;
import com.example.cakeshop.eventBus.MyUpdateCartEvent;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ICartListner {
    @BindView(R.id.main_layout)
    RelativeLayout mainLayout;
    @BindView(R.id.lblItemName)
    TextView txt_itemName;
    @BindView(R.id.lblItemPrice)
    TextView txtPricelable;
    @BindView(R.id.imgItemImage)
    ImageView img_itemImage;
    @BindView(R.id.badge)
    NotificationBadge badge;
    @BindView(R.id.btnCart)
    ImageView btnCart;
    @BindView(R.id.btnAddToCart)
    Button btnAddToCart;


    ICartListner icartListner;
    String size;
    String imageUri ="https://basketpay.in/wp-content/uploads/2020/12/Chocolate-Covered-Strawberry-Cake-2.jpg";
    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event){
        countCartItems();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        icartListner =this;


        Intent intent = getIntent();
        Glide.with(this).load(imageUri).into(img_itemImage);
         
        btnCart.setOnClickListener(v-> startActivity(new Intent(this,CartActivity.class)));

        btnAddToCart.setOnClickListener(v -> {
            final String name = txt_itemName.getText().toString();
            final String price = txtPricelable.getText().toString();

            if (TextUtils.isEmpty(size)){
                Toast.makeText(this,"Please Select size ",Toast.LENGTH_SHORT).show();
                return;
            }
            addToCart(name,price,imageUri);
        });


        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radSize);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                size = radioButton.getText().toString();
            }
        });
    }

    public void addToCart(String name,String price,String image) {

        DatabaseReference cart = FirebaseDatabase.getInstance().getReference("Cart").child("001");

        cart.child("0001").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){

                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Item already in cart")
                            .setMessage("Do you want to add another ! " )
                            .setNegativeButton("NO", (dialog1, which) -> dialog1.dismiss())
                            .setPositiveButton("YES", (dialog12, which) -> {


                                Cart cartmodel = snapshot.getValue(Cart.class);
                                Map<String,Object> updateData = new HashMap<>();
                                updateData.put("qty",cartmodel.getQty() + 1);
                                updateData.put("toatalPrice",(cartmodel.getQty() + 1) *Float.parseFloat(cartmodel.getPrice()));

                                cart.child("0001").updateChildren(updateData)
                                        .addOnSuccessListener(aVoid -> {
                                            Snackbar.make(mainLayout,"Order added to cart  ",Snackbar.LENGTH_LONG).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Snackbar.make(mainLayout,"Order added fail  ",Snackbar.LENGTH_LONG).show();
                                        });



                                dialog12.dismiss();
                            }).create();
                    dialog.show();


                }else{
                    Cart cartmodel = new Cart();
                    cartmodel.setKey("0001");
                    cartmodel.setPrice(price);
                    cartmodel.setName(name);
                    cartmodel.setImage(image);
                    cartmodel.setSize(size);
                    cartmodel.setQty(1);
                    cartmodel.setToatalPrice(Float.parseFloat(price));

                    cart.child("0001").setValue(cartmodel)
                            .addOnSuccessListener(aVoid -> Snackbar.make(mainLayout,"Order added to cart  ",Snackbar.LENGTH_LONG).show())
                            .addOnFailureListener(e -> {
                                Snackbar.make(mainLayout,"Order added to cart fail  ",Snackbar.LENGTH_LONG).show();
                            });
                }

                EventBus.getDefault().postSticky(new MyUpdateCartEvent());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                icartListner.onCartLoadFail(error.getMessage());

            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        countCartItems();
    }
    private void countCartItems() {
        List<Cart> cartModels = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference("Cart").child("001")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot cartSnapshot : snapshot.getChildren()){
                            Cart cartModel = cartSnapshot.getValue(Cart.class);
                            cartModel.setKey(cartSnapshot.getKey());
                            cartModels.add(cartModel);
                        }
                        icartListner.onCartLoadSuccess(cartModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        icartListner.onCartLoadFail(error.getMessage());
                    }
                });
    }

    @Override
    public void onCartLoadSuccess(List<Cart> cart) {
        int cartSum = 0;
        for(Cart cartModal : cart)
            cartSum += cartModal.getQty();

        badge.setNumber(cartSum);
    }

    @Override
    public void onCartLoadFail(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }
}