package com.example.demo.util;

import com.example.demo.model.persistence.Item;

public class ItemCount {
    Item item;
    Integer count;

    public ItemCount(Item item, Integer count) {
        this.item = item;
        this.count = count;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
