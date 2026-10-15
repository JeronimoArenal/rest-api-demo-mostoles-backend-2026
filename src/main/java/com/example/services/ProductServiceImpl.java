package com.example.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.dao.ProductDao;
import com.example.entities.Product;

import lombok.RequiredArgsConstructor;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    //............................ Constructor .......................................
    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return productDao.findAll(pageable);
    }

    @Override
    public List<Product> findAll(Sort sort) {
        return productDao.findAll(sort);
    }

    @Override
    public Product findById(int id) {
        return productDao.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productDao.save(product);
    }

    @Override
    public void delete(Product product) {
        productDao.delete(product);
    }

    @Override
    public List<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    public List<Product> findByPresentationName(String presentationName) {
        return productDao.findByPresentation_Name(presentationName);
    }

}
