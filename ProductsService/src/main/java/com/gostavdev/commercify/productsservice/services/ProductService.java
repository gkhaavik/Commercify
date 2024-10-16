package com.gostavdev.commercify.productsservice.services;

import com.gostavdev.commercify.productsservice.dto.ProductDTO;
import com.gostavdev.commercify.productsservice.dto.ProductDTOMapper;
import com.gostavdev.commercify.productsservice.entities.ProductEntity;
import com.gostavdev.commercify.productsservice.repositories.ProductRepository;
import com.gostavdev.commercify.productsservice.requests.ProductRequest;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductDTOMapper mapper;

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(mapper);
    }

    public Page<ProductDTO> getActiveProducts(Pageable pageable) {
        return productRepository.queryAllByActiveTrue(pageable).map(mapper);
    }

    public ProductDTO getProductById(Long id) {
        return productRepository.findById(id).map(mapper).orElseThrow(() -> new NoSuchElementException("Product not found"));
    }

    public ProductDTO saveProduct(ProductRequest request) {
        ProductEntity productEntity = ProductEntity.builder()
                .name(request.name())
                .description(request.description())
                .currency(request.currency())
                .unitPrice(request.unitPrice())
                .stock(request.stock())
                .active(true)
                .build();

        if (!Stripe.apiKey.equals("sk_missing")) {
            try {
                long amountInCents = (long) (productEntity.getUnitPrice() * 100);

                ProductCreateParams.DefaultPriceData defaultPriceData = ProductCreateParams.DefaultPriceData.builder()
                        .setCurrency(productEntity.getCurrency())
                        .setUnitAmount(amountInCents).build();

                ProductCreateParams params =
                        ProductCreateParams.builder()
                                .setName(productEntity.getName())
                                .setDescription(productEntity.getDescription())
                                .setDefaultPriceData(defaultPriceData)
                                .build();
                Product stripeProduct = Product.create(params);

                productEntity.setStripeId(stripeProduct.getId());

            } catch (StripeException e) {
                throw new RuntimeException("Stripe error", e);
            }
        }

        ProductEntity savedProduct = productRepository.save(productEntity);

        return mapper.apply(savedProduct);
    }

    public boolean deleteProduct(Long id, boolean forceDeletion) throws RuntimeException {
        System.out.println("Stripe key: " + Stripe.apiKey);
        ProductEntity productEnt = productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Product not found"));

        if (!productEnt.getActive()) {
            throw new RuntimeException("Product is already deleted");
        }

        if (!Stripe.apiKey.equals("sk_missing")) {
            return deleteProductFromStripe(productEnt, forceDeletion);
        }

        if (Stripe.apiKey.equals("sk_missing") && productEnt.getStripeId() != null) {
            throw new RuntimeException("Cant delete product from stripe without stripe key");
        }

        if (!forceDeletion) {
            System.out.println("Deactivating product");
            productEnt.setActive(false);
            productRepository.save(productEnt);
            return false;
        }

        productRepository.deleteById(id);
        System.out.println("Product deleted");
        return true;
    }

    private boolean deleteProductFromStripe(ProductEntity productEnt, boolean forceDeletion) {
        try {
            if (forceDeletion) {
                Product stripeProduct = Product.retrieve(productEnt.getStripeId());
                stripeProduct.delete();
                productRepository.deleteById(productEnt.getProductId());
                return true;
            }

            ProductUpdateParams params = ProductUpdateParams.builder()
                    .setActive(false)
                    .build();
            Product stripeProduct = Product.retrieve(productEnt.getStripeId());
            stripeProduct.update(params);
            productEnt.setActive(false);
            productRepository.save(productEnt);
            return false;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error", e);
        }
    }

    public List<ProductDTO> saveProducts(List<ProductRequest> request) {
        return request.stream().map(this::saveProduct).toList();
    }
}
