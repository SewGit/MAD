package com.example.cakeshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cakeshop.Adapter.CartAdapter;
import com.example.cakeshop.Listner.ICartListner;
import com.example.cakeshop.Modal.Cart;
import com.example.cakeshop.eventBus.MyUpdateCartEvent;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CartActivity extends AppCompatActivity implements ICartListner {
    @BindView(R.id.recycle_cart)
    RecyclerView recycle_cart;
    @BindView(R.id.main_layout)
    ConstraintLayout mainLayout;
    @BindView(R.id.btnBack)
    ImageView btnback;
    @BindView(R.id.lblTotalCartCost)
    TextView txtTotal;
    @BindView(R.id.btnPay)
    Button btnPay;


    ICartListner icartListner;

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
        loadCartFromFirebase();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        init();
        loadCartFromFirebase();
        btnback.setOnClickListener(v -> finish());
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this,PaymentActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadCartFromFirebase() {
        List<Cart> cartList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference("Cart")
                .child("001").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for (DataSnapshot dataSnapshot:snapshot.getChildren()){
                        Cart cartModel = dataSnapshot.getValue(Cart.class);
                        cartModel.setKey(dataSnapshot.getKey());
                        cartList.add(cartModel);
                    }
                    icartListner.onCartLoadSuccess(cartList);
                }else{
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Cart empty add items!",
                            Toast.LENGTH_LONG);
                    toast.show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                icartListner.onCartLoadFail(error.getMessage());
            }
        });
    }
    private void init(){
        ButterKnife.bind(this);

        icartListner =this;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recycle_cart.setLayoutManager(linearLayoutManager);
        recycle_cart.addItemDecoration(new DividerItemDecoration(this,linearLayoutManager.getOrientation()));

        btnback.setOnClickListener(v->finish());
    }

    @Override
    public void onCartLoadSuccess(List<Cart> cart) {
        double sum =0;
        for(Cart cartModal : cart){
            sum += cartModal.getToatalPrice();
        }
        txtTotal .setText(new StringBuffer("Total cost : Rs. ").append(sum));

        CartAdapter adapter = new CartAdapter(this,cart,this);
        recycle_cart.setAdapter(adapter);
    }

    @Override
    public void onCartLoadFail(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }
}