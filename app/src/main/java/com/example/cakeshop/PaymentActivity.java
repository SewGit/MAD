package com.example.cakeshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cakeshop.Adapter.CartAdapter;
import com.example.cakeshop.Listner.ICartListner;
import com.example.cakeshop.Modal.Cart;
import com.example.cakeshop.Modal.Payment;
import com.example.cakeshop.eventBus.MyUpdateCartEvent;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PaymentActivity extends AppCompatActivity implements ICartListner {

    @BindView(R.id.mainLayout)
    ConstraintLayout mainLayout;
    @BindView(R.id.btnBack)
    ImageView btnback;
    @BindView(R.id.lblTotal)
    TextView txtTotal;
    @BindView(R.id.btnPlaceOrder)
    Button btnPay;
    @BindView(R.id.txtCardNumber)
    EditText txtCardNumber;
    @BindView(R.id.txtCusName)
    EditText txtCusName;
    @BindView(R.id.txtDate)
    EditText txtDate;
    @BindView(R.id.txtPin)
    EditText txtPin;
    @BindView(R.id.btnCart)
    ImageView cartBtn;



    List<Cart> cartItemList;
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
        setContentView(R.layout.activity_payment);

        ButterKnife.bind(this);
        icartListner =this;
        loadCartFromFirebase();



        cartBtn.setOnClickListener(v -> startActivity(new Intent(PaymentActivity.this,CartActivity.class)));
        btnback.setOnClickListener(v->finish());
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override

            // validation
            public void onClick(View v) {
                final String cusName = txtCusName.getText().toString();
                final String cardNumber = txtCardNumber.getText().toString();
                final String cvv = txtPin.getText().toString();
                final String date = txtDate.getText().toString();

                if (TextUtils.isEmpty(cusName)) {
                    txtCusName.setError("Name is Required.");
                    return;
                }
                if (TextUtils.isEmpty(cardNumber)) {
                    txtCardNumber.setError("Card number is Required.");
                    return;
                }
                if (TextUtils.isEmpty(date)) {
                    txtDate.setError("Expire date is Required.");
                    return;
                }
                if (validateCardExpiryDate(date) == false) {
                    txtDate.setError("Enter valid date ex :- mm/yy.");
                    return;
                }
                if (TextUtils.isEmpty(cvv)) {
                    txtPin.setError("Card CVV is Required.");
                    return;
                }
                if (cvv.length() < 3 || cvv.length() > 4) {
                    txtPin.setError("Enter valid CVV number.");
                    return;
                }


                addPayment(cvv,cardNumber,cusName,date);
            }
        });
    }
    boolean validateCardExpiryDate(String date) {
        return date.matches("(?:0[1-9]|1[0-2])/[0-9]{2}");
    }
    //add payment
    private void addPayment(String cvv,String cardnumber,String cusName,String expireDate) {
        DatabaseReference reviewref = FirebaseDatabase.getInstance().getReference("Payment");
        Double totalSum = 0.0;
        for(Cart cartModal : cartItemList){
            totalSum += cartModal.getToatalPrice();
        }

        String key = reviewref.push().getKey();
        Payment payment = new Payment();
        payment.setCardCVV(cvv);
        payment.setCardNumber(cardnumber);
        payment.setCustomerName(cusName);
        payment.setExpireDate(expireDate);
        payment.setCartList(cartItemList);
        payment.setTotalCost(totalSum);

        reviewref.push().setValue(payment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,"payment added success",Toast.LENGTH_SHORT).show();
                    deleteFromFirebase();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,"payment added fail",Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFromFirebase() {

        FirebaseDatabase.getInstance().getReference("Cart").child("001")
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    EventBus.getDefault().postSticky(new MyUpdateCartEvent());
                    Toast.makeText(this,"Delete Item from cart",Toast.LENGTH_SHORT).show();

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
                    cartItemList = cartList;
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
    @Override
    public void onCartLoadSuccess(List<Cart> cart) {
        double sum =0;
        for(Cart cartModal : cart){
            sum += cartModal.getToatalPrice();
        }

        txtTotal.setText(new StringBuffer("Total cost : Rs. ").append(sum));

    }

    @Override
    public void onCartLoadFail(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }
}