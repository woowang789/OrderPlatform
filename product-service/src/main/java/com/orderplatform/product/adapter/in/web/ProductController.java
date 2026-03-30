package com.orderplatform.product.adapter.in.web;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.product.adapter.in.web.dto.CreateProductRequest;
import com.orderplatform.product.adapter.in.web.dto.ProductResponse;
import com.orderplatform.product.application.port.in.CreateProductCommand;
import com.orderplatform.product.application.port.in.CreateProductUseCase;
import com.orderplatform.product.application.port.in.GetProductUseCase;
import com.orderplatform.product.application.port.in.ProductInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @CurrentMemberId Long memberId) {
        CreateProductCommand command = new CreateProductCommand(
                request.name(), request.price(), request.stock(), request.category()
        );
        ProductInfo info = createProductUseCase.createProduct(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(info));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> responses = getProductUseCase.getProducts().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable @NonNull Long id) {
        ProductInfo info = getProductUseCase.getProduct(id);
        return ResponseEntity.ok(ProductResponse.from(info));
    }
}
