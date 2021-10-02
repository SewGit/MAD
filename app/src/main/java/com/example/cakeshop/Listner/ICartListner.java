package com.example.cakeshop.Listner;

import com.example.cakeshop.Modal.Cart;

import java.util.List;

public interface ICartListner {
    void onCartLoadSuccess(List<Cart> cart);
    void onCartLoadFail(String message);
}
